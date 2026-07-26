package com.odonta.polity.workflow;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.integration.invite.CardoInvitationDispatch;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationStatus;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.result.MembershipInvitationAcceptanceResult;
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
  private final PolityService polities;

  @Transactional
  public MembershipInvitationAcceptanceResult accept(UUID invitationId, AuthenticatedUser actor) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    IdentityUser identity = identityUsers.get(actor.id());
    MembershipInvitation invitation =
        invitations
            .findEntityByIdForUpdate(invitationId)
            .orElseThrow(PolityResource.MEMBERSHIP_INVITATION::notFound);
    if (invitation.getCardoInvitationId() == null
        || invitation.getInvitedUserId() == null
        || invitation.getCardoExpiresAt() == null) {
      throw ApiException.conflict(
          "invitation_not_ready", "This invitation is still being prepared. Try again shortly.");
    }
    requireInvitee(invitation, identity);
    if (invitation.getAcceptanceStatus() != null) {
      return MembershipInvitationAcceptanceResult.from(invitation);
    }
    if (invitation.getStatus() != MembershipInvitationStatus.PENDING) {
      throw PolityResource.MEMBERSHIP_INVITATION.notFound();
    }
    if (!invitation.getCardoExpiresAt().isAfter(now)) {
      throw ApiException.gone("invitation_expired", "This invitation has expired.");
    }
    polities.requireActive(invitation.getPolityId());
    if (memberships.existsByPolityIdAndUserIdAndStatus(
        invitation.getPolityId(), identity.id(), com.odonta.polity.model.MembershipStatus.ACTIVE)) {
      throw ApiException.conflict("member_exists", "This user is already a member.");
    }
    if (!invitation.requestAcceptance(now)) {
      return MembershipInvitationAcceptanceResult.from(invitation);
    }
    invitations.saveAndFlush(invitation);
    invitationDispatch.stageAcceptance(invitation.getId(), now);
    return MembershipInvitationAcceptanceResult.from(invitation);
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
}
