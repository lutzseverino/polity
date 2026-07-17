package com.odonta.polity.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipInvitationTest {

  @Test
  void cardoRegistrationIsIdempotentForTheSameInvitationAndIdentity() {
    MembershipInvitation invitation = invitation();
    UUID cardoInvitationId = UUID.randomUUID();
    UUID invitedUserId = UUID.randomUUID();
    OffsetDateTime expiresAt = OffsetDateTime.parse("2026-07-20T12:00:00Z");

    invitation.registerCardoInvitation(cardoInvitationId, invitedUserId, expiresAt);
    invitation.registerCardoInvitation(cardoInvitationId, invitedUserId, expiresAt);

    assertThat(invitation.getCardoInvitationId()).isEqualTo(cardoInvitationId);
    assertThat(invitation.getInvitedUserId()).isEqualTo(invitedUserId);
    assertThat(invitation.getCardoExpiresAt()).isEqualTo(expiresAt);
  }

  @Test
  void cardoRegistrationRejectsAConflictingRemoteInvitation() {
    MembershipInvitation invitation = invitation();
    invitation.registerCardoInvitation(
        UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.parse("2026-07-20T12:00:00Z"));

    assertThatThrownBy(
            () ->
                invitation.registerCardoInvitation(
                    UUID.randomUUID(),
                    invitation.getInvitedUserId(),
                    invitation.getCardoExpiresAt()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Membership invitation is linked to another Cardo invitation.");
  }

  @Test
  void cardoRegistrationRejectsAConflictingIdentity() {
    MembershipInvitation invitation = invitation();
    invitation.registerCardoInvitation(
        UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.parse("2026-07-20T12:00:00Z"));

    assertThatThrownBy(
            () ->
                invitation.registerCardoInvitation(
                    invitation.getCardoInvitationId(),
                    UUID.randomUUID(),
                    invitation.getCardoExpiresAt()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Membership invitation is linked to another invited user.");
  }

  private MembershipInvitation invitation() {
    return new MembershipInvitation(
        UUID.randomUUID(),
        "friend@example.com",
        UUID.randomUUID(),
        OffsetDateTime.parse("2026-07-17T12:00:00Z"));
  }
}
