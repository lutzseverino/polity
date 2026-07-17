package com.odonta.polity.integration.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.config.MembershipInvitationProperties;
import com.odonta.polity.repository.MembershipInvitationProjection;
import com.odonta.polity.repository.MembershipInvitationRepository;
import io.github.lutzseverino.cardo.invite.client.CreateInvitation;
import io.github.lutzseverino.cardo.invite.client.CreatedInvitation;
import io.github.lutzseverino.cardo.invite.client.Invitation;
import io.github.lutzseverino.cardo.invite.client.InvitationGrant;
import io.github.lutzseverino.cardo.invite.client.InvitationStatus;
import io.github.lutzseverino.cardo.invite.client.InvitationsClient;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CardoInvitationProcessorTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-17T12:00:00Z");
  private static final URI ACCEPT_URL =
      URI.create("https://polity.example.com/polities/invitations");

  private final InvitationsClient client = mock(InvitationsClient.class);
  private final MembershipInvitationRepository invitations =
      mock(MembershipInvitationRepository.class);
  private final CardoInvitationState state = mock(CardoInvitationState.class);
  private final CardoInvitationProcessor processor =
      new CardoInvitationProcessor(
          client, invitations, new MembershipInvitationProperties(ACCEPT_URL), state);

  @Test
  void createsCardoInvitationFromProductOwnedSnapshotAndRegistersIdentity() {
    UUID invitationId = UUID.randomUUID();
    UUID polityId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    UUID inviterUserId = UUID.randomUUID();
    UUID cardoInvitationId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    MembershipInvitationProjection local =
        invitation(invitationId, polityId, inviterMembershipId, "friend@example.com", null);
    when(invitations.findProjectedById(invitationId)).thenReturn(Optional.of(local));
    when(client.create(org.mockito.ArgumentMatchers.any(CreateInvitation.class)))
        .thenReturn(
            new CreatedInvitation(
                cardoInvitation(
                    cardoInvitationId, invitationId, polityId, inviteeUserId, inviterUserId),
                URI.create(ACCEPT_URL + "/secret-token")));

    processor.create(new CardoInvitationCreationRequested(invitationId, inviterUserId));

    ArgumentCaptor<CreateInvitation> input = ArgumentCaptor.forClass(CreateInvitation.class);
    verify(client).create(input.capture());
    assertThat(input.getValue().requestId()).isEqualTo(invitationId);
    assertThat(input.getValue().tenantId()).isEqualTo(polityId);
    assertThat(input.getValue().tenantResourceType()).isEqualTo("polity:polity");
    assertThat(input.getValue().email()).isEqualTo("friend@example.com");
    assertThat(input.getValue().accessProfile()).isEqualTo("polity:member");
    assertThat(input.getValue().grants())
        .containsExactly(
            new InvitationGrant("polity:polity", "read"),
            new InvitationGrant("polity:polity", "participate"));
    assertThat(input.getValue().invitedBy()).isEqualTo(inviterUserId);
    assertThat(input.getValue().acceptUrlBase()).isEqualTo(ACCEPT_URL);
    verify(state).register(invitationId, cardoInvitationId, inviteeUserId, NOW.plusDays(3));
  }

  @Test
  void acceptsUsingStoredCardoInvitationId() {
    UUID invitationId = UUID.randomUUID();
    UUID cardoInvitationId = UUID.randomUUID();
    MembershipInvitationProjection local =
        invitation(
            invitationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "friend@example.com",
            cardoInvitationId);
    when(invitations.findProjectedById(invitationId)).thenReturn(Optional.of(local));

    processor.accept(new CardoInvitationAcceptanceRequested(invitationId, NOW));

    verify(client).accept(cardoInvitationId, NOW);
  }

  @Test
  void acceptanceRemainsRetryableUntilCardoCreationIsRegistered() {
    UUID invitationId = UUID.randomUUID();
    MembershipInvitationProjection local =
        invitation(invitationId, UUID.randomUUID(), UUID.randomUUID(), "friend@example.com", null);
    when(invitations.findProjectedById(invitationId)).thenReturn(Optional.of(local));

    assertThatThrownBy(
            () -> processor.accept(new CardoInvitationAcceptanceRequested(invitationId, NOW)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cardo invitation has not been created yet.");

    verify(client, never())
        .accept(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void creationDoesNotRegisterAMismatchedCardoResponse() {
    UUID invitationId = UUID.randomUUID();
    UUID polityId = UUID.randomUUID();
    UUID inviterUserId = UUID.randomUUID();
    MembershipInvitationProjection local =
        invitation(invitationId, polityId, UUID.randomUUID(), "friend@example.com", null);
    when(invitations.findProjectedById(invitationId)).thenReturn(Optional.of(local));
    Invitation mismatched =
        cardoInvitation(
            UUID.randomUUID(), UUID.randomUUID(), polityId, UUID.randomUUID(), inviterUserId);
    when(client.create(org.mockito.ArgumentMatchers.any(CreateInvitation.class)))
        .thenReturn(new CreatedInvitation(mismatched, URI.create(ACCEPT_URL + "/secret-token")));

    assertThatThrownBy(
            () ->
                processor.create(new CardoInvitationCreationRequested(invitationId, inviterUserId)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cardo invitation creation response does not match the requested invitation.");

    verify(state, never())
        .register(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  private MembershipInvitationProjection invitation(
      UUID invitationId,
      UUID polityId,
      UUID inviterMembershipId,
      String email,
      UUID cardoInvitationId) {
    MembershipInvitationProjection invitation = mock(MembershipInvitationProjection.class);
    when(invitation.getId()).thenReturn(invitationId);
    when(invitation.getPolityId()).thenReturn(polityId);
    when(invitation.getInvitedBy()).thenReturn(inviterMembershipId);
    when(invitation.getEmail()).thenReturn(email);
    when(invitation.getCardoInvitationId()).thenReturn(cardoInvitationId);
    return invitation;
  }

  private Invitation cardoInvitation(
      UUID id, UUID requestId, UUID polityId, UUID inviteeUserId, UUID inviterUserId) {
    return new Invitation(
        id,
        requestId,
        polityId,
        "polity:polity",
        "polity:member",
        "friend@example.com",
        inviteeUserId,
        inviterUserId,
        InvitationStatus.PENDING,
        NOW.plusDays(3),
        null,
        null,
        NOW,
        NOW);
  }
}
