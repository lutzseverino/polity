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
import com.odonta.polity.workflow.CompleteMembershipInvitationWorkflow;
import io.github.lutzseverino.cardo.common.api.ApiException;
import io.github.lutzseverino.cardo.identity.client.IdentityUser;
import io.github.lutzseverino.cardo.identity.client.IdentityUserStatus;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import io.github.lutzseverino.cardo.invite.client.CreateInvitation;
import io.github.lutzseverino.cardo.invite.client.CreatedInvitation;
import io.github.lutzseverino.cardo.invite.client.Invitation;
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
  private final IdentityUsersClient identityUsers = mock(IdentityUsersClient.class);
  private final MembershipInvitationRepository invitations =
      mock(MembershipInvitationRepository.class);
  private final CardoInvitationState state = mock(CardoInvitationState.class);
  private final CompleteMembershipInvitationWorkflow completion =
      mock(CompleteMembershipInvitationWorkflow.class);
  private final CardoInvitationProcessor processor =
      new CardoInvitationProcessor(
          client,
          identityUsers,
          invitations,
          new MembershipInvitationProperties(ACCEPT_URL),
          state,
          completion);

  @Test
  void createsLifecycleOnlyCardoInvitationAndRegistersIdentity() {
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
    UUID polityId = local.getPolityId();
    UUID invitedUserId = local.getInvitedUserId();
    String email = local.getEmail();
    IdentityUser identity = identity(invitedUserId);
    when(identityUsers.get(invitedUserId)).thenReturn(identity);
    when(client.accept(cardoInvitationId, NOW))
        .thenReturn(
            new Invitation(
                cardoInvitationId,
                invitationId,
                polityId,
                "polity:polity",
                email,
                invitedUserId,
                UUID.randomUUID(),
                InvitationStatus.ACCEPTED,
                NOW.plusDays(3),
                NOW,
                null,
                NOW.minusDays(1),
                NOW));

    processor.accept(new CardoInvitationAcceptanceRequested(invitationId, NOW));

    verify(client).accept(cardoInvitationId, NOW);
    verify(completion).complete(invitationId, identity, NOW);
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
  void terminalCardoAcceptanceFailureIsPersistedWithoutRetry() {
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
    when(client.accept(cardoInvitationId, NOW))
        .thenThrow(ApiException.gone("invitation_expired", "Expired"));

    processor.accept(new CardoInvitationAcceptanceRequested(invitationId, NOW));

    verify(state).fail(invitationId, "invitation_expired");
    verify(completion, never())
        .complete(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
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
    when(invitation.getInvitedUserId()).thenReturn(UUID.randomUUID());
    return invitation;
  }

  private Invitation cardoInvitation(
      UUID id, UUID requestId, UUID polityId, UUID inviteeUserId, UUID inviterUserId) {
    return new Invitation(
        id,
        requestId,
        polityId,
        "polity:polity",
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

  private IdentityUser identity(UUID id) {
    return new IdentityUser(
        id,
        "subject:friend",
        "friend@example.com",
        "Friend",
        null,
        IdentityUserStatus.ACTIVE,
        true,
        NOW.minusDays(2),
        NOW.minusDays(1));
  }
}
