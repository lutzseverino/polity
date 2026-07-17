package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.integration.invite.CardoInvitationDispatch;
import com.odonta.polity.mapper.MembershipApplicationMapper;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationStatus;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.service.OfficialRecordService;
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
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AcceptMembershipInvitationWorkflowTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-16T02:00:00Z");

  private final CardoInvitationDispatch invitationDispatch = mock(CardoInvitationDispatch.class);
  private final IdentityUsersClient identityUsers = mock(IdentityUsersClient.class);
  private final MembershipInvitationRepository invitations =
      mock(MembershipInvitationRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final PolityBootstrapCompleter bootstrap = mock(PolityBootstrapCompleter.class);
  private final PolityContextResolver polityContext = mock(PolityContextResolver.class);
  private final PolityService polities = mock(PolityService.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final AcceptMembershipInvitationWorkflow workflow =
      new AcceptMembershipInvitationWorkflow(
          Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
          invitationDispatch,
          identityUsers,
          invitations,
          memberships,
          Mappers.getMapper(MembershipApplicationMapper.class),
          bootstrap,
          polityContext,
          polities,
          officialRecords);

  @Test
  void acceptCreatesMembershipAndStagesCardoAcceptance() {
    UUID polityId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    UUID admittedMembershipId = UUID.randomUUID();
    MembershipInvitation invitation =
        invitation(polityId, invitationId, inviteeUserId, inviterMembershipId);
    IdentityUser invitee = identity(inviteeUserId);
    ConstitutionVersion constitution = constitution(polityId);
    Jurisdiction jurisdiction = jurisdiction(polityId);
    when(identityUsers.get(inviteeUserId)).thenReturn(invitee);
    when(invitations.findEntityByIdAndStatus(invitationId, MembershipInvitationStatus.PENDING))
        .thenReturn(Optional.of(invitation));
    when(memberships.saveAndFlush(any(Membership.class)))
        .thenAnswer(
            invocation -> {
              Membership member = invocation.getArgument(0);
              ReflectionTestUtils.setField(member, "id", admittedMembershipId);
              return member;
            });
    stubAdmissionRecordContext(polityId, constitution, jurisdiction);
    MembershipProjection projection = memberProjection(admittedMembershipId, inviteeUserId);
    when(memberships.findProjectedById(admittedMembershipId)).thenReturn(Optional.of(projection));

    var result =
        workflow.accept(
            invitationId, new AuthenticatedUser(inviteeUserId, "subject:invitee", "Bea"));

    assertThat(result.id()).isEqualTo(admittedMembershipId);
    assertThat(invitation.getStatus()).isEqualTo(MembershipInvitationStatus.ACCEPTED);
    assertThat(invitation.getRespondedAt()).isEqualTo(NOW);
    ArgumentCaptor<Membership> memberCaptor = ArgumentCaptor.forClass(Membership.class);
    verify(memberships).saveAndFlush(memberCaptor.capture());
    assertThat(memberCaptor.getValue().getAdmittedBy()).isEqualTo(inviterMembershipId);
    verify(invitationDispatch).stageAcceptance(invitationId, NOW);
    verify(bootstrap).completeIfReady(polityId, NOW);
    verify(officialRecords)
        .append(
            eq(polityId),
            eq(jurisdiction.getId()),
            eq(constitution.getId()),
            eq(admittedMembershipId),
            eq(OfficialRecordType.MEMBER_ADMITTED),
            eq(admittedMembershipId),
            any(),
            any(),
            eq(NOW));
  }

  @Test
  void acceptReactivatesResignedMembership() {
    UUID polityId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    UUID admittedMembershipId = UUID.randomUUID();
    Membership resigned = member(polityId, inviteeUserId, admittedMembershipId);
    resigned.resign(NOW.minusDays(1));
    MembershipInvitation invitation =
        invitation(polityId, invitationId, inviteeUserId, inviterMembershipId);
    ConstitutionVersion constitution = constitution(polityId);
    Jurisdiction jurisdiction = jurisdiction(polityId);
    when(identityUsers.get(inviteeUserId)).thenReturn(identity(inviteeUserId));
    when(invitations.findEntityByIdAndStatus(invitationId, MembershipInvitationStatus.PENDING))
        .thenReturn(Optional.of(invitation));
    when(memberships.findEntityByPolityIdAndUserId(polityId, inviteeUserId))
        .thenReturn(Optional.of(resigned));
    when(memberships.saveAndFlush(resigned)).thenReturn(resigned);
    stubAdmissionRecordContext(polityId, constitution, jurisdiction);
    MembershipProjection projection = memberProjection(admittedMembershipId, inviteeUserId);
    when(memberships.findProjectedById(admittedMembershipId)).thenReturn(Optional.of(projection));

    var result =
        workflow.accept(
            invitationId, new AuthenticatedUser(inviteeUserId, "subject:invitee", "Bea"));

    assertThat(result.id()).isEqualTo(admittedMembershipId);
    assertThat(resigned.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    assertThat(resigned.getAuthorizationSubject()).isEqualTo("subject:invitee");
    assertThat(resigned.getEmail()).isEqualTo("friend@example.com");
    assertThat(resigned.getDisplayName()).isEqualTo("Bea");
    assertThat(resigned.getAdmittedAt()).isEqualTo(NOW);
    assertThat(resigned.getAdmittedBy()).isEqualTo(inviterMembershipId);
    assertThat(resigned.getResignedAt()).isNull();
    verify(invitationDispatch).stageAcceptance(invitationId, NOW);
    verify(officialRecords)
        .append(
            eq(polityId),
            eq(jurisdiction.getId()),
            eq(constitution.getId()),
            eq(admittedMembershipId),
            eq(OfficialRecordType.MEMBER_ADMITTED),
            eq(admittedMembershipId),
            any(),
            any(),
            eq(NOW));
  }

  @Test
  void acceptRejectsAlreadyActiveMembership() {
    UUID polityId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    Membership active = member(polityId, inviteeUserId, UUID.randomUUID());
    MembershipInvitation invitation =
        invitation(polityId, invitationId, inviteeUserId, UUID.randomUUID());
    when(identityUsers.get(inviteeUserId)).thenReturn(identity(inviteeUserId));
    when(invitations.findEntityByIdAndStatus(invitationId, MembershipInvitationStatus.PENDING))
        .thenReturn(Optional.of(invitation));
    when(memberships.findEntityByPolityIdAndUserId(polityId, inviteeUserId))
        .thenReturn(Optional.of(active));

    assertThatThrownBy(
            () ->
                workflow.accept(
                    invitationId, new AuthenticatedUser(inviteeUserId, "subject:invitee", "Bea")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This user is already a member.");

    verify(memberships, never()).saveAndFlush(any());
    verify(invitationDispatch, never()).stageAcceptance(any(), any());
  }

  @Test
  void acceptWaitsUntilCardoCreationIsRegistered() {
    UUID polityId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    MembershipInvitation invitation =
        new MembershipInvitation(
            polityId, "friend@example.com", UUID.randomUUID(), NOW.minusDays(1));
    ReflectionTestUtils.setField(invitation, "id", invitationId);
    when(identityUsers.get(inviteeUserId)).thenReturn(identity(inviteeUserId));
    when(invitations.findEntityByIdAndStatus(invitationId, MembershipInvitationStatus.PENDING))
        .thenReturn(Optional.of(invitation));

    assertThatThrownBy(
            () ->
                workflow.accept(
                    invitationId, new AuthenticatedUser(inviteeUserId, "subject:invitee", "Bea")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This invitation is still being prepared. Try again shortly.");

    verify(memberships, never()).saveAndFlush(any());
    verify(invitationDispatch, never()).stageAcceptance(any(), any());
  }

  @Test
  void acceptRejectsAnExpiredCardoInvitationBeforeAdmission() {
    UUID polityId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    MembershipInvitation invitation =
        new MembershipInvitation(
            polityId, "friend@example.com", UUID.randomUUID(), NOW.minusDays(4));
    ReflectionTestUtils.setField(invitation, "id", invitationId);
    invitation.registerCardoInvitation(UUID.randomUUID(), inviteeUserId, NOW);
    when(identityUsers.get(inviteeUserId)).thenReturn(identity(inviteeUserId));
    when(invitations.findEntityByIdAndStatus(invitationId, MembershipInvitationStatus.PENDING))
        .thenReturn(Optional.of(invitation));

    assertThatThrownBy(
            () ->
                workflow.accept(
                    invitationId, new AuthenticatedUser(inviteeUserId, "subject:invitee", "Bea")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This invitation has expired.");

    verify(memberships, never()).saveAndFlush(any());
    verify(invitationDispatch, never()).stageAcceptance(any(), any());
  }

  private void stubAdmissionRecordContext(
      UUID polityId, ConstitutionVersion constitution, Jurisdiction jurisdiction) {
    when(polityContext.constitution(polityId)).thenReturn(constitution);
    when(polityContext.rootJurisdiction(polityId)).thenReturn(jurisdiction);
  }

  private MembershipInvitation invitation(
      UUID polityId, UUID invitationId, UUID inviteeUserId, UUID inviterMembershipId) {
    MembershipInvitation invitation =
        new MembershipInvitation(
            polityId, "friend@example.com", inviterMembershipId, NOW.minusDays(1));
    ReflectionTestUtils.setField(invitation, "id", invitationId);
    invitation.registerCardoInvitation(UUID.randomUUID(), inviteeUserId, NOW.plusDays(2));
    return invitation;
  }

  private IdentityUser identity(UUID userId) {
    return new IdentityUser(
        userId,
        "subject:invitee",
        "friend@example.com",
        "Bea",
        null,
        IdentityUserStatus.ACTIVE,
        true,
        NOW.minusDays(10),
        NOW.minusDays(1));
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

  private MembershipProjection memberProjection(UUID membershipId, UUID userId) {
    MembershipProjection projection = mock(MembershipProjection.class);
    when(projection.getId()).thenReturn(membershipId);
    when(projection.getUserId()).thenReturn(userId);
    when(projection.getDisplayName()).thenReturn("Bea");
    when(projection.getEmail()).thenReturn("friend@example.com");
    when(projection.getStatus()).thenReturn(MembershipStatus.ACTIVE);
    when(projection.getAdmittedAt()).thenReturn(NOW);
    return projection;
  }
}
