package com.odonta.polity.integration.invite;

import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.repository.MembershipInvitationRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
class CardoInvitationState {
  private final MembershipInvitationRepository invitations;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void register(
      UUID invitationId, UUID cardoInvitationId, UUID invitedUserId, OffsetDateTime expiresAt) {
    MembershipInvitation invitation =
        invitations
            .findEntityById(invitationId)
            .orElseThrow(() -> new IllegalStateException("Membership invitation not found."));
    invitation.registerCardoInvitation(cardoInvitationId, invitedUserId, expiresAt);
    invitations.saveAndFlush(invitation);
  }
}
