package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.mapper.MembershipInvitationApplicationMapper;
import com.odonta.polity.model.MembershipInvitationStatus;
import com.odonta.polity.repository.MembershipInvitationProjection;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.result.MembershipInvitationCompletionStatus;
import com.odonta.polity.result.MembershipInvitationResult;
import io.github.lutzseverino.cardo.common.api.ApiException;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import io.github.lutzseverino.cardo.invite.client.InvitationCompletion;
import io.github.lutzseverino.cardo.invite.client.InvitationCompletionStatus;
import io.github.lutzseverino.cardo.invite.client.InvitationToken;
import io.github.lutzseverino.cardo.invite.client.InvitationsClient;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class MembershipInvitationServiceTest {
  private final InvitationsClient cardoInvitations = mock(InvitationsClient.class);
  private final IdentityUsersClient identityUsers = mock(IdentityUsersClient.class);
  private final MembershipInvitationRepository invitations =
      mock(MembershipInvitationRepository.class);
  private final MembershipService memberships = mock(MembershipService.class);
  private final PolityContextResolver polityContext = mock(PolityContextResolver.class);
  private final MembershipInvitationService service =
      new MembershipInvitationService(
          cardoInvitations,
          identityUsers,
          invitations,
          memberships,
          Mappers.getMapper(MembershipInvitationApplicationMapper.class),
          polityContext);

  @Test
  void tokenInspectionReturnsOnlyVerifiedPolityOnboardingContext() {
    UUID cardoInvitationId = UUID.randomUUID();
    UUID polityId = UUID.randomUUID();
    OffsetDateTime expiresAt = OffsetDateTime.parse("2026-07-20T10:00:00Z");
    MembershipInvitationProjection local = linkedInvitation(cardoInvitationId, polityId);
    when(cardoInvitations.getByToken("secret-token"))
        .thenReturn(
            new InvitationToken(
                cardoInvitationId, polityId, "polity:polity", "friend@example.com", expiresAt));
    when(invitations.findProjectedByCardoInvitationId(cardoInvitationId))
        .thenReturn(Optional.of(local));
    when(polityContext.name(polityId)).thenReturn("Friend Republic");

    var result = service.getByToken("secret-token");

    assertThat(result.polityId()).isEqualTo(polityId);
    assertThat(result.polityName()).isEqualTo("Friend Republic");
    assertThat(result.invitedEmail()).isEqualTo("friend@example.com");
    assertThat(result.expiresAt()).isEqualTo(expiresAt);
  }

  @Test
  void identityCompletionRequestVerifiesTheLocalInvitationAndReturnsDurableState() {
    UUID cardoInvitationId = UUID.randomUUID();
    UUID polityId = UUID.randomUUID();
    when(cardoInvitations.getByToken("secret-token"))
        .thenReturn(
            new InvitationToken(
                cardoInvitationId,
                polityId,
                "polity:polity",
                "friend@example.com",
                OffsetDateTime.parse("2026-07-20T10:00:00Z")));
    MembershipInvitationProjection local = linkedInvitation(cardoInvitationId, polityId);
    when(invitations.findProjectedByCardoInvitationId(cardoInvitationId))
        .thenReturn(Optional.of(local));
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-18T10:00:00Z");
    when(cardoInvitations.requestCompletion("secret-token"))
        .thenReturn(
            new InvitationCompletion(
                UUID.randomUUID(),
                cardoInvitationId,
                UUID.randomUUID(),
                InvitationCompletionStatus.REQUESTED,
                0,
                null,
                null,
                null,
                createdAt,
                createdAt));

    var result = service.requestCompletion("secret-token");

    assertThat(result.status()).isEqualTo(MembershipInvitationCompletionStatus.REQUESTED);
    assertThat(result.attemptCount()).isZero();
    assertThat(result.createdAt()).isEqualTo(createdAt);
    verify(cardoInvitations).requestCompletion("secret-token");
  }

  @Test
  void identityCompletionDoesNotProxyAForeignProductToken() {
    UUID cardoInvitationId = UUID.randomUUID();
    when(cardoInvitations.getByToken("foreign-token"))
        .thenReturn(
            new InvitationToken(
                cardoInvitationId,
                UUID.randomUUID(),
                "another-product:tenant",
                "friend@example.com",
                OffsetDateTime.parse("2026-07-20T10:00:00Z")));
    assertThatThrownBy(() -> service.requestCompletion("foreign-token"))
        .isInstanceOf(ApiException.class)
        .hasMessage("This invitation belongs to another product.");

    verify(cardoInvitations, never()).requestCompletion(any());
  }

  @Test
  void identityCompletionDoesNotProxyAnUnlinkedCardoToken() {
    UUID cardoInvitationId = UUID.randomUUID();
    when(cardoInvitations.getByToken("unlinked-token"))
        .thenReturn(
            new InvitationToken(
                cardoInvitationId,
                UUID.randomUUID(),
                "polity:polity",
                "friend@example.com",
                OffsetDateTime.parse("2026-07-20T10:00:00Z")));
    when(invitations.findProjectedByCardoInvitationId(cardoInvitationId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requestCompletion("unlinked-token"))
        .isInstanceOf(ApiException.class);

    verify(cardoInvitations, never()).requestCompletion(any());
  }

  @Test
  void completionPollingPreservesFailureAndActionExpiryDetails() {
    UUID cardoInvitationId = UUID.randomUUID();
    UUID polityId = UUID.randomUUID();
    OffsetDateTime actionExpiresAt = OffsetDateTime.parse("2026-07-18T10:15:00Z");
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-18T10:00:00Z");
    OffsetDateTime updatedAt = OffsetDateTime.parse("2026-07-18T10:02:00Z");
    when(cardoInvitations.getByToken("secret-token"))
        .thenReturn(
            new InvitationToken(
                cardoInvitationId,
                polityId,
                "polity:polity",
                "friend@example.com",
                OffsetDateTime.parse("2026-07-20T10:00:00Z")));
    MembershipInvitationProjection local = linkedInvitation(cardoInvitationId, polityId);
    when(invitations.findProjectedByCardoInvitationId(cardoInvitationId))
        .thenReturn(Optional.of(local));
    when(cardoInvitations.getCompletion("secret-token"))
        .thenReturn(
            new InvitationCompletion(
                UUID.randomUUID(),
                cardoInvitationId,
                UUID.randomUUID(),
                InvitationCompletionStatus.FAILED,
                3,
                "credential_action_expired",
                actionExpiresAt,
                null,
                createdAt,
                updatedAt));

    var result = service.getCompletion("secret-token");

    assertThat(result.status()).isEqualTo(MembershipInvitationCompletionStatus.FAILED);
    assertThat(result.attemptCount()).isEqualTo(3);
    assertThat(result.lastError()).isEqualTo("credential_action_expired");
    assertThat(result.actionExpiresAt()).isEqualTo(actionExpiresAt);
    assertThat(result.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void getAssemblesTheCanonicalInvitationReadFromOwningSources() {
    UUID invitationId = UUID.randomUUID();
    UUID polityId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    OffsetDateTime invitedAt = OffsetDateTime.parse("2026-07-16T10:00:00Z");
    MembershipInvitationProjection projection = mock(MembershipInvitationProjection.class);
    when(projection.getId()).thenReturn(invitationId);
    when(projection.getPolityId()).thenReturn(polityId);
    when(projection.getEmail()).thenReturn("friend@example.com");
    when(projection.getInvitedBy()).thenReturn(inviterMembershipId);
    when(projection.getStatus()).thenReturn(MembershipInvitationStatus.PENDING);
    when(projection.getInvitedAt()).thenReturn(invitedAt);
    when(invitations.findProjectedById(invitationId)).thenReturn(Optional.of(projection));
    when(polityContext.name(polityId)).thenReturn("Friend Republic");
    when(memberships.displayName(inviterMembershipId)).thenReturn("Ada");

    MembershipInvitationResult result = service.get(invitationId);

    assertThat(result.id()).isEqualTo(invitationId);
    assertThat(result.polityId()).isEqualTo(polityId);
    assertThat(result.polityName()).isEqualTo("Friend Republic");
    assertThat(result.email()).isEqualTo("friend@example.com");
    assertThat(result.invitedByName()).isEqualTo("Ada");
    assertThat(result.status()).isEqualTo(MembershipInvitationStatus.PENDING);
    assertThat(result.invitedAt()).isEqualTo(invitedAt);
  }

  private MembershipInvitationProjection linkedInvitation(UUID cardoInvitationId, UUID polityId) {
    MembershipInvitationProjection projection = mock(MembershipInvitationProjection.class);
    when(projection.getCardoInvitationId()).thenReturn(cardoInvitationId);
    when(projection.getPolityId()).thenReturn(polityId);
    when(projection.getEmail()).thenReturn("friend@example.com");
    when(projection.getStatus()).thenReturn(MembershipInvitationStatus.PENDING);
    return projection;
  }
}
