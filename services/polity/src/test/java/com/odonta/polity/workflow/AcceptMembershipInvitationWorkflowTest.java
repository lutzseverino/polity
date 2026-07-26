package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.odonta.polity.integration.invite.CardoInvitationDispatch;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationAcceptanceStatus;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.service.PolityService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import io.github.lutzseverino.cardo.identity.client.IdentityUser;
import io.github.lutzseverino.cardo.identity.client.IdentityUserStatus;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AcceptMembershipInvitationWorkflowTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-16T02:00:00Z");

  private final CardoInvitationDispatch dispatch = mock(CardoInvitationDispatch.class);
  private final IdentityUsersClient identities = mock(IdentityUsersClient.class);
  private final MembershipInvitationRepository invitations =
      mock(MembershipInvitationRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final PolityService polities = mock(PolityService.class);
  private final AcceptMembershipInvitationWorkflow workflow =
      new AcceptMembershipInvitationWorkflow(
          Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
          dispatch,
          identities,
          invitations,
          memberships,
          polities);

  @Test
  void persistsOriginalAcceptanceIntentBeforePublishingOnce() {
    UUID invitationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    MembershipInvitation invitation = invitation(invitationId, userId);
    when(identities.get(userId)).thenReturn(identity(userId));
    when(invitations.findEntityByIdForUpdate(invitationId)).thenReturn(Optional.of(invitation));
    when(memberships.existsByPolityIdAndUserIdAndStatus(
            invitation.getPolityId(), userId, MembershipStatus.ACTIVE))
        .thenReturn(false);

    var first = workflow.accept(invitationId, actor(userId));
    var repeated = workflow.accept(invitationId, actor(userId));

    assertThat(first.status()).isEqualTo(MembershipInvitationAcceptanceStatus.REQUESTED);
    assertThat(first.requestedAt()).isEqualTo(NOW);
    assertThat(repeated).isEqualTo(first);
    verify(dispatch).stageAcceptance(invitationId, NOW);
    verify(invitations).saveAndFlush(invitation);
    verifyNoMoreInteractions(dispatch);
  }

  @Test
  void rejectsAcceptanceUntilCardoCreationIsRegistered() {
    UUID invitationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    MembershipInvitation invitation =
        new MembershipInvitation(
            UUID.randomUUID(), "friend@example.com", UUID.randomUUID(), NOW.minusDays(1));
    ReflectionTestUtils.setField(invitation, "id", invitationId);
    when(identities.get(userId)).thenReturn(identity(userId));
    when(invitations.findEntityByIdForUpdate(invitationId)).thenReturn(Optional.of(invitation));

    assertThatThrownBy(() -> workflow.accept(invitationId, actor(userId)))
        .isInstanceOf(ApiException.class)
        .hasMessage("This invitation is still being prepared. Try again shortly.");

    verify(invitations, never()).saveAndFlush(any());
    verify(dispatch, never()).stageAcceptance(any(), any());
  }

  @Test
  void rejectsExpiredInvitationBeforePublishingAcceptance() {
    UUID invitationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    MembershipInvitation invitation = invitation(invitationId, userId, NOW);
    when(identities.get(userId)).thenReturn(identity(userId));
    when(invitations.findEntityByIdForUpdate(invitationId)).thenReturn(Optional.of(invitation));

    assertThatThrownBy(() -> workflow.accept(invitationId, actor(userId)))
        .isInstanceOf(ApiException.class)
        .hasMessage("This invitation has expired.");

    verify(invitations, never()).saveAndFlush(any());
    verify(dispatch, never()).stageAcceptance(any(), any());
  }

  @Test
  void rejectsAuthenticatedUserWhoDoesNotOwnInvitation() {
    UUID invitationId = UUID.randomUUID();
    UUID invitedUserId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    MembershipInvitation invitation = invitation(invitationId, invitedUserId);
    when(identities.get(actorId))
        .thenReturn(
            new IdentityUser(
                actorId,
                "subject:other",
                "other@example.com",
                "Other",
                null,
                IdentityUserStatus.ACTIVE,
                true,
                NOW.minusDays(2),
                NOW.minusDays(1)));
    when(invitations.findEntityByIdForUpdate(invitationId)).thenReturn(Optional.of(invitation));

    assertThatThrownBy(() -> workflow.accept(invitationId, actor(actorId)))
        .isInstanceOf(ApiException.class)
        .hasMessage("This invitation belongs to another user.");

    verify(invitations, never()).saveAndFlush(any());
    verify(dispatch, never()).stageAcceptance(any(), any());
  }

  @Test
  void rejectsAlreadyActiveMemberBeforePublishingAcceptance() {
    UUID invitationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    MembershipInvitation invitation = invitation(invitationId, userId);
    when(identities.get(userId)).thenReturn(identity(userId));
    when(invitations.findEntityByIdForUpdate(invitationId)).thenReturn(Optional.of(invitation));
    when(memberships.existsByPolityIdAndUserIdAndStatus(
            invitation.getPolityId(), userId, MembershipStatus.ACTIVE))
        .thenReturn(true);

    assertThatThrownBy(() -> workflow.accept(invitationId, actor(userId)))
        .isInstanceOf(ApiException.class)
        .hasMessage("This user is already a member.");

    verify(invitations, never()).saveAndFlush(any());
    verify(dispatch, never()).stageAcceptance(any(), any());
  }

  private MembershipInvitation invitation(UUID id, UUID userId) {
    return invitation(id, userId, NOW.plusDays(1));
  }

  private MembershipInvitation invitation(UUID id, UUID userId, OffsetDateTime expiresAt) {
    MembershipInvitation invitation =
        new MembershipInvitation(UUID.randomUUID(), "friend@example.com", UUID.randomUUID(), NOW);
    ReflectionTestUtils.setField(invitation, "id", id);
    invitation.registerCardoInvitation(UUID.randomUUID(), userId, expiresAt);
    return invitation;
  }

  private AuthenticatedUser actor(UUID userId) {
    return new AuthenticatedUser(userId, "subject:friend", "Friend");
  }

  private IdentityUser identity(UUID userId) {
    return new IdentityUser(
        userId,
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
