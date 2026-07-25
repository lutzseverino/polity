package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationAcceptanceStatus;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.service.OfficialRecordService;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceipt;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceiptStatus;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import io.github.lutzseverino.cardo.identity.client.IdentityUser;
import io.github.lutzseverino.cardo.identity.client.IdentityUserStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CompleteMembershipInvitationWorkflowTest {
  private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-07-20T10:00:00Z");
  private static final OffsetDateTime COMPLETED_AT = OffsetDateTime.parse("2026-07-20T10:01:00Z");

  private final Grants grants = mock(Grants.class);
  private final MembershipInvitationRepository invitations =
      mock(MembershipInvitationRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final OfficialRecordService records = mock(OfficialRecordService.class);
  private final PolityBootstrapCompleter bootstrap = mock(PolityBootstrapCompleter.class);
  private final PolityContextResolver context = mock(PolityContextResolver.class);
  private final CompleteMembershipInvitationWorkflow workflow =
      new CompleteMembershipInvitationWorkflow(
          Clock.fixed(COMPLETED_AT.toInstant(), ZoneOffset.UTC),
          grants,
          invitations,
          memberships,
          records,
          new PolityGrantPlanner(),
          bootstrap,
          context);

  @Test
  void invitedMembershipAndReceiptAreCommittedAsOneCompletion() {
    Fixture fixture = fixture(null);

    workflow.complete(fixture.invitation.getId(), fixture.identity, REQUESTED_AT);

    assertThat(fixture.invitation.getAcceptanceStatus())
        .isEqualTo(MembershipInvitationAcceptanceStatus.COMPLETED);
    assertThat(fixture.savedMembership().getGrantReceiptId()).isEqualTo(fixture.receipt.id());
    assertThat(fixture.savedMembership().getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    verify(grants).stage(any());
    verify(invitations).saveAndFlush(fixture.invitation);
  }

  @Test
  void reactivationReplacesRevokedAccessWithOneNewReceipt() {
    Membership resigned =
        new Membership(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "old-subject",
            "old@example.com",
            "Old",
            REQUESTED_AT.minusDays(2),
            null);
    ReflectionTestUtils.setField(resigned, "id", UUID.randomUUID());
    resigned.retainGrantReceipt(UUID.randomUUID());
    resigned.resign(REQUESTED_AT.minusDays(1));
    Fixture fixture = fixture(resigned);

    workflow.complete(fixture.invitation.getId(), fixture.identity, REQUESTED_AT);

    assertThat(resigned.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    assertThat(resigned.getGrantReceiptId()).isEqualTo(fixture.receipt.id());
    assertThat(resigned.getAuthorizationSubject())
        .isEqualTo(fixture.identity.authorizationSubject());
    verify(grants).stage(any());
    verify(memberships).saveAndFlush(resigned);
  }

  @Test
  void retryAfterLocalCompletionDoesNotStageDuplicateWork() {
    Fixture fixture = fixture(null);
    workflow.complete(fixture.invitation.getId(), fixture.identity, REQUESTED_AT);

    workflow.complete(fixture.invitation.getId(), fixture.identity, REQUESTED_AT);

    verify(grants).stage(any());
  }

  private Fixture fixture(Membership existing) {
    UUID invitationId = UUID.randomUUID();
    UUID polityId = existing == null ? UUID.randomUUID() : existing.getPolityId();
    UUID userId = existing == null ? UUID.randomUUID() : existing.getUserId();
    MembershipInvitation invitation =
        new MembershipInvitation(
            polityId, "friend@example.com", UUID.randomUUID(), REQUESTED_AT.minusDays(1));
    ReflectionTestUtils.setField(invitation, "id", invitationId);
    invitation.registerCardoInvitation(UUID.randomUUID(), userId, REQUESTED_AT.plusDays(1));
    invitation.requestAcceptance(REQUESTED_AT);
    IdentityUser identity =
        new IdentityUser(
            userId,
            "subject:friend",
            "friend@example.com",
            "Friend",
            null,
            IdentityUserStatus.ACTIVE,
            true,
            REQUESTED_AT.minusDays(2),
            REQUESTED_AT.minusDays(1));
    GrantReceipt receipt = new GrantReceipt(UUID.randomUUID(), GrantReceiptStatus.PENDING, null);
    when(invitations.findEntityByIdForUpdate(invitationId)).thenReturn(Optional.of(invitation));
    when(memberships.findEntityByPolityIdAndUserIdForUpdate(polityId, userId))
        .thenReturn(Optional.ofNullable(existing));
    when(grants.stage(any())).thenReturn(receipt);
    Membership[] saved = new Membership[1];
    when(memberships.saveAndFlush(any(Membership.class)))
        .thenAnswer(
            invocation -> {
              Membership membership = invocation.getArgument(0);
              if (membership.getId() == null) {
                ReflectionTestUtils.setField(membership, "id", UUID.randomUUID());
              }
              saved[0] = membership;
              return membership;
            });
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Constitution", "Body", REQUESTED_AT.minusDays(2));
    ReflectionTestUtils.setField(constitution, "id", UUID.randomUUID());
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Root", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    when(context.constitution(polityId)).thenReturn(constitution);
    when(context.rootJurisdiction(polityId)).thenReturn(jurisdiction);

    return new Fixture(invitation, identity, receipt, saved);
  }

  private static final class Fixture {
    private final MembershipInvitation invitation;
    private final IdentityUser identity;
    private final GrantReceipt receipt;
    private final Membership[] saved;

    private Fixture(
        MembershipInvitation invitation,
        IdentityUser identity,
        GrantReceipt receipt,
        Membership[] saved) {
      this.invitation = invitation;
      this.identity = identity;
      this.receipt = receipt;
      this.saved = saved;
    }

    Membership savedMembership() {
      return saved[0];
    }
  }
}
