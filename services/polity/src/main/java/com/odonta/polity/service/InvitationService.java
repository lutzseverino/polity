package com.odonta.polity.service;

import com.odonta.authorization.grant.Grants;
import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.identity.client.IdentityUser;
import com.odonta.identity.client.IdentityUsersClient;
import com.odonta.identity.client.ProvisionalUser;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.input.CreateMemberInvitationInput;
import com.odonta.polity.mapper.MembershipApplicationMapper;
import com.odonta.polity.mapper.MembershipInvitationApplicationMapper;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.InvitationStatus;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.MembershipInvitationProjection;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.resolver.PolityActionAvailabilityResolver;
import com.odonta.polity.result.MembershipInvitationResult;
import com.odonta.polity.result.MembershipResult;
import com.odonta.polity.result.PageResult;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class InvitationService {
  private final Clock clock;
  private final ConstitutionalAuthority authority;
  private final Grants grants;
  private final IdentityUsersClient identityUsers;
  private final MembershipInvitationRepository invitations;
  private final MembershipService membershipService;
  private final MembershipRepository memberships;
  private final MembershipInvitationApplicationMapper invitationMapper;
  private final MembershipApplicationMapper memberMapper;
  private final PolityGrantPlanner grantPlanner;
  private final PolityService polities;
  private final PolityActionAvailabilityResolver actionAvailability;
  private final OfficialRecordService officialRecords;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MembershipInvitationResult create(
      UUID polityId, AuthenticatedUser actor, @Valid CreateMemberInvitationInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    String email = normalize(input.email());
    Membership inviter = membershipService.active(polityId, actor.id());
    var constitution = polities.constitution(polityId);
    requireAdmissionAuthority(inviter, constitution);
    if (invitations.existsByPolityIdAndEmailIgnoreCaseAndStatus(
        polityId, email, InvitationStatus.PENDING)) {
      throw ApiException.conflict(
          "invitation_exists", "This user already has a pending invitation.");
    }
    ProvisionalUser invitee = identityUsers.createProvisional(email);
    memberships
        .findEntityByPolityIdAndUserId(polityId, invitee.id())
        .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
        .ifPresent(
            membership -> {
              throw ApiException.conflict("member_exists", "This user is already a member.");
            });
    if (invitations.existsByPolityIdAndInvitedUserIdAndStatus(
        polityId, invitee.id(), InvitationStatus.PENDING)) {
      throw ApiException.conflict(
          "invitation_exists", "This user already has a pending invitation.");
    }
    MembershipInvitation invitation =
        invitations.saveAndFlush(
            new MembershipInvitation(
                polityId,
                invitee.id(),
                invitee.authorizationSubject(),
                email,
                inviter.getId(),
                now));
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    officialRecords.append(
        polityId,
        jurisdiction.getId(),
        constitution.getId(),
        inviter.getId(),
        OfficialRecordType.MEMBER_INVITED,
        invitation.getId(),
        OfficialRecordContext.none(),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.MEMBER_INVITED,
            TemplateParameters.of(
                "inviterName", inviter.getDisplayName(), "inviteeEmail", invitation.getEmail())),
        now);
    return result(invitation.getId());
  }

  @PreAuthorize(PolityPermissions.HAS_POLITY_READ)
  public PageResult<MembershipInvitationResult> listPolityInvitations(
      UUID polityId, UUID userId, int page, int size) {
    membershipService.active(polityId, userId);
    Page<MembershipInvitationProjection> projections =
        invitations.findProjectionsByPolityIdOrderByInvitedAtDescIdAsc(
            polityId, PageRequest.of(page, size));
    return new PageResult<>(
        projections.stream().map(this::result).toList(),
        projections.getNumber(),
        projections.getSize(),
        projections.getTotalElements());
  }

  public PageResult<MembershipInvitationResult> listCurrentUserInvitations(
      AuthenticatedUser actor, int page, int size) {
    IdentityUser user = identityUsers.get(actor.id());
    Page<MembershipInvitationProjection> projections =
        invitations.findPendingProjectionsForInvitee(
            user.id(),
            List.of(normalize(user.email())),
            InvitationStatus.PENDING,
            PageRequest.of(page, size));
    return new PageResult<>(
        projections.stream().map(this::result).toList(),
        projections.getNumber(),
        projections.getSize(),
        projections.getTotalElements());
  }

  @Transactional
  public MembershipResult accept(UUID invitationId, AuthenticatedUser actor) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    IdentityUser identity = identityUsers.get(actor.id());
    MembershipInvitation invitation =
        invitations
            .findEntityByIdAndStatus(invitationId, InvitationStatus.PENDING)
            .orElseThrow(
                () -> ApiException.notFound("invitation_not_found", "Invitation not found."));
    polities.requireActive(invitation.getPolityId());
    requireInvitee(invitation, identity);
    Membership admitted =
        memberships
            .findEntityByPolityIdAndUserId(invitation.getPolityId(), identity.id())
            .map(
                membership -> {
                  if (membership.getStatus() == MembershipStatus.ACTIVE) {
                    throw ApiException.conflict("member_exists", "This user is already a member.");
                  }
                  membership.reactivate(
                      identity.authorizationSubject(),
                      identity.email(),
                      displayName(identity),
                      now,
                      invitation.getInvitedBy());
                  return memberships.saveAndFlush(membership);
                })
            .orElseGet(
                () ->
                    memberships.saveAndFlush(
                        new Membership(
                            invitation.getPolityId(),
                            identity.id(),
                            identity.authorizationSubject(),
                            identity.email(),
                            displayName(identity),
                            now,
                            invitation.getInvitedBy())));
    invitation.accept(now);
    invitations.saveAndFlush(invitation);
    grants.stage(
        grantPlanner.membership(identity.authorizationSubject(), invitation.getPolityId()));
    polities.completeBootstrapIfReady(invitation.getPolityId(), now);
    var constitution = polities.constitution(invitation.getPolityId());
    Jurisdiction jurisdiction = polities.jurisdiction(invitation.getPolityId());
    officialRecords.append(
        invitation.getPolityId(),
        jurisdiction.getId(),
        constitution.getId(),
        admitted.getId(),
        OfficialRecordType.MEMBER_ADMITTED,
        admitted.getId(),
        OfficialRecordContext.none(),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.MEMBER_ADMITTED,
            TemplateParameters.of("memberName", admitted.getDisplayName())),
        now);
    return memberMapper.toResult(
        memberships
            .findProjectedById(admitted.getId())
            .orElseThrow(() -> ApiException.notFound("member_not_found", "Member not found.")));
  }

  private MembershipInvitationResult result(UUID invitationId) {
    return result(
        invitations
            .findProjectedById(invitationId)
            .orElseThrow(
                () -> ApiException.notFound("invitation_not_found", "Invitation not found.")));
  }

  private MembershipInvitationResult result(MembershipInvitationProjection projection) {
    return invitationMapper.toResult(
        projection,
        polities.name(projection.getPolityId()),
        membershipService.displayName(projection.getInvitedBy()));
  }

  private void requireAdmissionAuthority(Membership inviter, ConstitutionVersion constitution) {
    try {
      authority.require(inviter, constitution, PowerCode.ADMIT_MEMBER);
    } catch (ApiException exception) {
      if (!exception.code().equals("constitutional_authority_missing")
          || !actionAvailability.hasProvisionalFounderAdmissionAuthority(inviter)) {
        throw exception;
      }
    }
  }

  private void requireInvitee(MembershipInvitation invitation, IdentityUser identity) {
    if (invitation.getInvitedUserId().equals(identity.id())
        || invitation.getEmail().equals(normalize(identity.email()))) {
      return;
    }
    throw ApiException.forbidden(
        "invitation_wrong_user", "This invitation belongs to another user.");
  }

  private String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String displayName(IdentityUser user) {
    return user.name() == null || user.name().isBlank() ? user.email() : user.name();
  }
}
