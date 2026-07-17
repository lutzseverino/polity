package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.input.CreateMembershipInvitationInput;
import com.odonta.polity.integration.invite.CardoInvitationDispatch;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationStatus;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.resolver.ActiveMembershipResolver;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.result.MembershipInvitationResult;
import com.odonta.polity.service.MembershipInvitationService;
import com.odonta.polity.service.OfficialRecordService;
import com.odonta.polity.service.PolityActionAvailabilityService;
import com.odonta.polity.service.PolityService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class CreateMembershipInvitationWorkflowTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-16T02:00:00Z");

  private final ConstitutionalAuthority authority = mock(ConstitutionalAuthority.class);
  private final CardoInvitationDispatch invitationDispatch = mock(CardoInvitationDispatch.class);
  private final MembershipInvitationRepository invitations =
      mock(MembershipInvitationRepository.class);
  private final ActiveMembershipResolver activeMemberships = mock(ActiveMembershipResolver.class);
  private final MembershipRepository membershipRepository = mock(MembershipRepository.class);
  private final MembershipInvitationService invitationResults =
      mock(MembershipInvitationService.class);
  private final PolityContextResolver polityContext = mock(PolityContextResolver.class);
  private final PolityService polities = mock(PolityService.class);
  private final PolityActionAvailabilityService actionAvailability =
      mock(PolityActionAvailabilityService.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final CreateMembershipInvitationWorkflow workflow =
      new CreateMembershipInvitationWorkflow(
          Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
          authority,
          invitationDispatch,
          invitations,
          activeMemberships,
          membershipRepository,
          invitationResults,
          polityContext,
          polities,
          actionAvailability,
          officialRecords);

  @Test
  void createPersistsPolityInvitationAndStagesCardoCreation() {
    InvitationFixture fixture = fixture();
    stubSuccessfulCreation(fixture);

    MembershipInvitationResult result = create(fixture);

    assertThat(result.status()).isEqualTo(MembershipInvitationStatus.PENDING);
    ArgumentCaptor<MembershipInvitation> invitationCaptor =
        ArgumentCaptor.forClass(MembershipInvitation.class);
    verify(invitations).saveAndFlush(invitationCaptor.capture());
    assertThat(invitationCaptor.getValue().getEmail()).isEqualTo("friend@example.com");
    assertThat(invitationCaptor.getValue().getInvitedUserId()).isNull();
    assertThat(invitationCaptor.getValue().getCardoInvitationId()).isNull();
    verify(invitationDispatch).stageCreation(fixture.invitationId(), fixture.inviterUserId());
    verify(membershipRepository, never()).saveAndFlush(any(Membership.class));
    verify(officialRecords)
        .append(
            eq(fixture.polityId()),
            eq(fixture.jurisdiction().getId()),
            eq(fixture.constitution().getId()),
            eq(fixture.inviterMembershipId()),
            eq(OfficialRecordType.MEMBER_INVITED),
            eq(fixture.invitationId()),
            any(),
            any(),
            eq(NOW));
  }

  @Test
  void createAllowsProvisionalFounderAdmission() {
    InvitationFixture fixture = fixture();
    when(activeMemberships.resolve(fixture.polityId(), fixture.inviterUserId()))
        .thenReturn(fixture.inviter());
    when(polityContext.constitution(fixture.polityId())).thenReturn(fixture.constitution());
    doThrow(
            ApiException.forbidden(
                "constitutional_authority_missing", "The member lacks constitutional authority."))
        .when(authority)
        .require(fixture.inviter(), fixture.constitution(), PowerCode.ADMIT_MEMBER);
    when(actionAvailability.hasProvisionalFounderAdmissionAuthority(
            fixture.polityId(), fixture.inviterUserId()))
        .thenReturn(true);
    stubCreationPersistence(fixture);

    MembershipInvitationResult result = create(fixture);

    assertThat(result.status()).isEqualTo(MembershipInvitationStatus.PENDING);
    verify(invitationDispatch).stageCreation(fixture.invitationId(), fixture.inviterUserId());
  }

  @Test
  void createRejectsAlreadyActiveMemberByNormalizedEmail() {
    InvitationFixture fixture = fixture();
    when(activeMemberships.resolve(fixture.polityId(), fixture.inviterUserId()))
        .thenReturn(fixture.inviter());
    when(polityContext.constitution(fixture.polityId())).thenReturn(fixture.constitution());
    when(membershipRepository.existsByPolityIdAndEmailIgnoreCaseAndStatus(
            fixture.polityId(), "friend@example.com", MembershipStatus.ACTIVE))
        .thenReturn(true);

    assertThatThrownBy(() -> create(fixture))
        .isInstanceOf(ApiException.class)
        .hasMessage("This user is already a member.");

    verify(invitations, never()).saveAndFlush(any());
    verify(invitationDispatch, never()).stageCreation(any(), any());
  }

  @Test
  void createDoesNotBypassMissingAdmissionPower() {
    InvitationFixture fixture = fixture();
    when(activeMemberships.resolve(fixture.polityId(), fixture.inviterUserId()))
        .thenReturn(fixture.inviter());
    when(polityContext.constitution(fixture.polityId())).thenReturn(fixture.constitution());
    doThrow(
            ApiException.forbidden(
                "constitutional_power_missing",
                "The governing constitution does not authorize this action."))
        .when(authority)
        .require(fixture.inviter(), fixture.constitution(), PowerCode.ADMIT_MEMBER);

    assertThatThrownBy(() -> create(fixture))
        .isInstanceOf(ApiException.class)
        .hasMessage("The governing constitution does not authorize this action.");

    verify(actionAvailability, never())
        .hasProvisionalFounderAdmissionAuthority(fixture.polityId(), fixture.inviterUserId());
    verify(invitationDispatch, never()).stageCreation(any(), any());
  }

  private MembershipInvitationResult create(InvitationFixture fixture) {
    return workflow.create(
        fixture.polityId(),
        new AuthenticatedUser(fixture.inviterUserId(), "subject:inviter", "Ada"),
        new CreateMembershipInvitationInput("Friend@Example.com"));
  }

  private void stubSuccessfulCreation(InvitationFixture fixture) {
    when(activeMemberships.resolve(fixture.polityId(), fixture.inviterUserId()))
        .thenReturn(fixture.inviter());
    when(polityContext.constitution(fixture.polityId())).thenReturn(fixture.constitution());
    stubCreationPersistence(fixture);
  }

  private void stubCreationPersistence(InvitationFixture fixture) {
    when(polityContext.rootJurisdiction(fixture.polityId())).thenReturn(fixture.jurisdiction());
    when(invitations.saveAndFlush(any(MembershipInvitation.class)))
        .thenAnswer(
            invocation -> {
              MembershipInvitation invitation = invocation.getArgument(0);
              ReflectionTestUtils.setField(invitation, "id", fixture.invitationId());
              return invitation;
            });
    when(invitationResults.get(fixture.invitationId()))
        .thenReturn(
            new MembershipInvitationResult(
                fixture.invitationId(),
                fixture.polityId(),
                "Friend Republic",
                "friend@example.com",
                "Ada",
                MembershipInvitationStatus.PENDING,
                NOW,
                null));
  }

  private InvitationFixture fixture() {
    UUID polityId = UUID.randomUUID();
    UUID inviterUserId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    return new InvitationFixture(
        polityId,
        inviterUserId,
        inviterMembershipId,
        UUID.randomUUID(),
        member(polityId, inviterUserId, inviterMembershipId),
        constitution(polityId),
        jurisdiction(polityId));
  }

  private Membership member(UUID polityId, UUID userId, UUID membershipId) {
    Membership member =
        new Membership(
            polityId,
            userId,
            "subject:" + userId,
            "ada@example.com",
            "Ada",
            NOW.minusDays(1),
            null);
    ReflectionTestUtils.setField(member, "id", membershipId);
    return member;
  }

  private ConstitutionVersion constitution(UUID polityId) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", "Body", NOW.minusDays(3));
    ReflectionTestUtils.setField(constitution, "id", UUID.randomUUID());
    return constitution;
  }

  private Jurisdiction jurisdiction(UUID polityId) {
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    return jurisdiction;
  }

  private record InvitationFixture(
      UUID polityId,
      UUID inviterUserId,
      UUID inviterMembershipId,
      UUID invitationId,
      Membership inviter,
      ConstitutionVersion constitution,
      Jurisdiction jurisdiction) {}
}
