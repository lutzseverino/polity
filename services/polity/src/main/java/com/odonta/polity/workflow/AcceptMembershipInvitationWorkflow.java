package com.odonta.polity.workflow;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.integration.invite.CardoInvitationDispatch;
import com.odonta.polity.mapper.MembershipApplicationMapper;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationStatus;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.result.MembershipResult;
import com.odonta.polity.service.OfficialRecordService;
import com.odonta.polity.service.PolityService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import io.github.lutzseverino.cardo.identity.client.IdentityUser;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AcceptMembershipInvitationWorkflow {
  private final Clock clock;
  private final CardoInvitationDispatch invitationDispatch;
  private final IdentityUsersClient identityUsers;
  private final MembershipInvitationRepository invitations;
  private final MembershipRepository memberships;
  private final MembershipApplicationMapper memberMapper;
  private final PolityBootstrapCompleter bootstrap;
  private final PolityContextResolver polityContext;
  private final PolityService polities;
  private final OfficialRecordService officialRecords;

  @Transactional
  public MembershipResult accept(UUID invitationId, AuthenticatedUser actor) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    IdentityUser identity = identityUsers.get(actor.id());
    MembershipInvitation invitation =
        invitations
            .findEntityByIdAndStatus(invitationId, MembershipInvitationStatus.PENDING)
            .orElseThrow(PolityResource.MEMBERSHIP_INVITATION::notFound);
    if (invitation.getCardoInvitationId() == null
        || invitation.getInvitedUserId() == null
        || invitation.getCardoExpiresAt() == null) {
      throw ApiException.conflict(
          "invitation_not_ready", "This invitation is still being prepared. Try again shortly.");
    }
    if (!invitation.getCardoExpiresAt().isAfter(now)) {
      throw ApiException.gone("invitation_expired", "This invitation has expired.");
    }
    polities.requireActive(invitation.getPolityId());
    requireInvitee(invitation, identity);
    Membership admitted = admit(invitation, identity, now);
    invitation.accept(now);
    invitations.saveAndFlush(invitation);
    bootstrap.completeIfReady(invitation.getPolityId(), now);
    recordAdmission(invitation, admitted, now);
    invitationDispatch.stageAcceptance(invitation.getId(), now);
    return memberMapper.toResult(
        memberships
            .findProjectedById(admitted.getId())
            .orElseThrow(PolityResource.MEMBER::notFound));
  }

  private Membership admit(
      MembershipInvitation invitation, IdentityUser identity, OffsetDateTime now) {
    return memberships
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
  }

  private void recordAdmission(
      MembershipInvitation invitation, Membership admitted, OffsetDateTime now) {
    var constitution = polityContext.constitution(invitation.getPolityId());
    Jurisdiction jurisdiction = polityContext.rootJurisdiction(invitation.getPolityId());
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
