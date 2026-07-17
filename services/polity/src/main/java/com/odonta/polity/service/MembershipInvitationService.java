package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.mapper.MembershipInvitationApplicationMapper;
import com.odonta.polity.model.MembershipInvitationStatus;
import com.odonta.polity.repository.MembershipInvitationProjection;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.result.MembershipInvitationCompletionResult;
import com.odonta.polity.result.MembershipInvitationCompletionStatus;
import com.odonta.polity.result.MembershipInvitationResult;
import com.odonta.polity.result.MembershipInvitationTokenResult;
import com.odonta.polity.result.PageResult;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import io.github.lutzseverino.cardo.identity.client.IdentityUser;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import io.github.lutzseverino.cardo.invite.client.InvitationCompletion;
import io.github.lutzseverino.cardo.invite.client.InvitationToken;
import io.github.lutzseverino.cardo.invite.client.InvitationsClient;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class MembershipInvitationService {
  private final InvitationsClient cardoInvitations;
  private final IdentityUsersClient identityUsers;
  private final MembershipInvitationRepository invitations;
  private final MembershipService memberships;
  private final MembershipInvitationApplicationMapper mapper;
  private final PolityContextResolver polityContext;

  @PreAuthorize(PolityPermissions.HAS_POLITY_READ)
  public PageResult<MembershipInvitationResult> listPolityMembershipInvitations(
      UUID polityId, UUID userId, int page, int size) {
    memberships.requireActive(polityId, userId);
    Page<MembershipInvitationProjection> projections =
        invitations.findProjectionsByPolityIdOrderByInvitedAtDescIdAsc(
            polityId, PageRequest.of(page, size));
    return new PageResult<>(
        projections.stream().map(this::result).toList(),
        projections.getNumber(),
        projections.getSize(),
        projections.getTotalElements());
  }

  public PageResult<MembershipInvitationResult> listCurrentUserMembershipInvitations(
      AuthenticatedUser actor, int page, int size) {
    IdentityUser user = identityUsers.get(actor.id());
    Page<MembershipInvitationProjection> projections =
        invitations.findPendingProjectionsForInvitee(
            user.id(),
            List.of(normalize(user.email())),
            MembershipInvitationStatus.PENDING,
            PageRequest.of(page, size));
    return new PageResult<>(
        projections.stream().map(this::result).toList(),
        projections.getNumber(),
        projections.getSize(),
        projections.getTotalElements());
  }

  public MembershipInvitationResult get(UUID invitationId) {
    return result(
        invitations
            .findProjectedById(invitationId)
            .orElseThrow(PolityResource.MEMBERSHIP_INVITATION::notFound));
  }

  public MembershipInvitationTokenResult getByToken(@NotBlank String token) {
    InvitationToken cardo = requireCardoToken(token);
    return new MembershipInvitationTokenResult(
        cardo.tenantId(),
        polityContext.name(cardo.tenantId()),
        cardo.invitedEmail(),
        cardo.expiresAt());
  }

  public MembershipInvitationCompletionResult requestCompletion(@NotBlank String token) {
    requireCardoToken(token);
    return completion(cardoInvitations.requestCompletion(token));
  }

  public MembershipInvitationCompletionResult getCompletion(@NotBlank String token) {
    requireCardoToken(token);
    return completion(cardoInvitations.getCompletion(token));
  }

  private InvitationToken requireCardoToken(String token) {
    InvitationToken cardo = cardoInvitations.getByToken(token);
    if (!PolityPermissions.POLITY_RESOURCE.equals(cardo.tenantResourceType())) {
      throw ApiException.forbidden(
          "invitation_product_mismatch", "This invitation belongs to another product.");
    }
    MembershipInvitationProjection local =
        invitations
            .findProjectedByCardoInvitationId(cardo.id())
            .orElseThrow(PolityResource.MEMBERSHIP_INVITATION::notFound);
    if (!MembershipInvitationStatus.PENDING.equals(local.getStatus())) {
      throw ApiException.gone("invitation_unavailable", "Invitation is no longer available.");
    }
    if (!local.getPolityId().equals(cardo.tenantId())
        || !normalize(local.getEmail()).equals(normalize(cardo.invitedEmail()))) {
      throw ApiException.conflict(
          "invitation_link_mismatch", "Cardo and Polity invitation records do not match.");
    }
    return cardo;
  }

  private MembershipInvitationResult result(MembershipInvitationProjection projection) {
    return mapper.toResult(
        projection,
        polityContext.name(projection.getPolityId()),
        memberships.displayName(projection.getInvitedBy()));
  }

  private MembershipInvitationCompletionResult completion(InvitationCompletion completion) {
    return new MembershipInvitationCompletionResult(
        MembershipInvitationCompletionStatus.valueOf(completion.status().name()),
        completion.attemptCount(),
        completion.lastError(),
        completion.actionExpiresAt(),
        completion.completedAt(),
        completion.createdAt(),
        completion.updatedAt());
  }

  private String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
