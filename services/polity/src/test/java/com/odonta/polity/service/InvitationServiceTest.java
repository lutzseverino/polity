package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.authorization.grant.Grants;
import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.identity.client.IdentityUser;
import com.odonta.identity.client.IdentityUserStatus;
import com.odonta.identity.client.IdentityUsersClient;
import com.odonta.identity.client.ProvisionalUser;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.input.CreateMemberInvitationInput;
import com.odonta.polity.mapper.MembershipApplicationMapper;
import com.odonta.polity.mapper.MembershipInvitationApplicationMapper;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.InvitationStatus;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.repository.MembershipInvitationProjection;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.resolver.PolityActionAvailabilityResolver;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class InvitationServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-16T02:00:00Z");

  private final ConstitutionalAuthority authority = mock(ConstitutionalAuthority.class);
  private final Grants grants = mock(Grants.class);
  private final IdentityUsersClient identityUsers = mock(IdentityUsersClient.class);
  private final MembershipInvitationRepository invitations =
      mock(MembershipInvitationRepository.class);
  private final MembershipService membershipService = mock(MembershipService.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final PolityGrantPlanner grantPlanner = new PolityGrantPlanner();
  private final PolityService polities = mock(PolityService.class);
  private final PolityActionAvailabilityResolver actionAvailability =
      mock(PolityActionAvailabilityResolver.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private InvitationService service;

  @BeforeEach
  void setUp() {
    when(membershipService.displayName(any(UUID.class))).thenReturn("Ada");
    when(polities.name(any(UUID.class))).thenReturn("Friend Republic");
    service =
        new InvitationService(
            Clock.fixed(Instant.from(NOW), ZoneOffset.UTC),
            authority,
            grants,
            identityUsers,
            invitations,
            membershipService,
            memberships,
            Mappers.getMapper(MembershipInvitationApplicationMapper.class),
            Mappers.getMapper(MembershipApplicationMapper.class),
            grantPlanner,
            polities,
            actionAvailability,
            officialRecords);
  }

  @Test
  void creatingInvitationDoesNotCreateMembershipOrGrantAccess() {
    UUID polityId = UUID.randomUUID();
    UUID inviterUserId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    Membership inviter = member(polityId, inviterUserId, inviterMembershipId);
    ConstitutionVersion constitution = constitution(polityId);
    Jurisdiction jurisdiction = jurisdiction(polityId);
    ProvisionalUser invitee = new ProvisionalUser(inviteeUserId, "subject:invitee");
    MembershipInvitationProjection invitationProjection =
        invitationProjection(invitationId, polityId);

    when(membershipService.active(polityId, inviterUserId)).thenReturn(inviter);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(identityUsers.createProvisional("friend@example.com")).thenReturn(invitee);
    when(invitations.saveAndFlush(any(MembershipInvitation.class)))
        .thenAnswer(
            invocation -> {
              MembershipInvitation invitation = invocation.getArgument(0);
              ReflectionTestUtils.setField(invitation, "id", invitationId);
              return invitation;
            });
    when(invitations.findProjectedById(invitationId)).thenReturn(Optional.of(invitationProjection));

    var result =
        service.create(
            polityId,
            new AuthenticatedUser(inviterUserId, "subject:inviter", "Ada"),
            new CreateMemberInvitationInput("Friend@Example.com"));

    assertThat(result.status()).isEqualTo(InvitationStatus.PENDING);
    ArgumentCaptor<MembershipInvitation> invitationCaptor =
        ArgumentCaptor.forClass(MembershipInvitation.class);
    verify(invitations).saveAndFlush(invitationCaptor.capture());
    assertThat(invitationCaptor.getValue().getEmail()).isEqualTo("friend@example.com");
    verify(memberships, never()).saveAndFlush(any(Membership.class));
    verify(grants, never()).stage(any());
    verify(officialRecords)
        .append(
            eq(polityId),
            eq(jurisdiction.getId()),
            eq(constitution.getId()),
            eq(inviterMembershipId),
            eq(OfficialRecordType.MEMBER_INVITED),
            eq(invitationId),
            any(),
            any(),
            eq(NOW));
  }

  @Test
  void creatingInvitationAllowsProvisionalFounderAdmission() {
    UUID polityId = UUID.randomUUID();
    UUID inviterUserId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    Membership inviter = member(polityId, inviterUserId, inviterMembershipId);
    ConstitutionVersion constitution = constitution(polityId);
    Jurisdiction jurisdiction = jurisdiction(polityId);
    ProvisionalUser invitee = new ProvisionalUser(inviteeUserId, "subject:invitee");
    MembershipInvitationProjection invitationProjection =
        invitationProjection(invitationId, polityId);

    when(membershipService.active(polityId, inviterUserId)).thenReturn(inviter);
    when(polities.constitution(polityId)).thenReturn(constitution);
    doThrow(
            ApiException.forbidden(
                "constitutional_authority_missing", "The member lacks constitutional authority."))
        .when(authority)
        .require(inviter, constitution, PowerCode.ADMIT_MEMBER);
    when(actionAvailability.hasProvisionalFounderAdmissionAuthority(inviter)).thenReturn(true);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(identityUsers.createProvisional("friend@example.com")).thenReturn(invitee);
    when(invitations.saveAndFlush(any(MembershipInvitation.class)))
        .thenAnswer(
            invocation -> {
              MembershipInvitation invitation = invocation.getArgument(0);
              ReflectionTestUtils.setField(invitation, "id", invitationId);
              return invitation;
            });
    when(invitations.findProjectedById(invitationId)).thenReturn(Optional.of(invitationProjection));

    var result =
        service.create(
            polityId,
            new AuthenticatedUser(inviterUserId, "subject:inviter", "Ada"),
            new CreateMemberInvitationInput("Friend@Example.com"));

    assertThat(result.status()).isEqualTo(InvitationStatus.PENDING);
    verify(actionAvailability).hasProvisionalFounderAdmissionAuthority(inviter);
    verify(invitations).saveAndFlush(any(MembershipInvitation.class));
  }

  @Test
  void creatingInvitationAllowsResignedMemberToBeInvitedAgain() {
    UUID polityId = UUID.randomUUID();
    UUID inviterUserId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    Membership inviter = member(polityId, inviterUserId, inviterMembershipId);
    Membership resigned = member(polityId, inviteeUserId, UUID.randomUUID());
    resigned.resign(NOW.minusDays(1));
    ConstitutionVersion constitution = constitution(polityId);
    Jurisdiction jurisdiction = jurisdiction(polityId);
    ProvisionalUser invitee = new ProvisionalUser(inviteeUserId, "subject:invitee");
    MembershipInvitationProjection invitationProjection =
        invitationProjection(invitationId, polityId);

    when(membershipService.active(polityId, inviterUserId)).thenReturn(inviter);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(identityUsers.createProvisional("friend@example.com")).thenReturn(invitee);
    when(memberships.findEntityByPolityIdAndUserId(polityId, inviteeUserId))
        .thenReturn(Optional.of(resigned));
    when(invitations.saveAndFlush(any(MembershipInvitation.class)))
        .thenAnswer(
            invocation -> {
              MembershipInvitation invitation = invocation.getArgument(0);
              ReflectionTestUtils.setField(invitation, "id", invitationId);
              return invitation;
            });
    when(invitations.findProjectedById(invitationId)).thenReturn(Optional.of(invitationProjection));

    var result =
        service.create(
            polityId,
            new AuthenticatedUser(inviterUserId, "subject:inviter", "Ada"),
            new CreateMemberInvitationInput("Friend@Example.com"));

    assertThat(result.status()).isEqualTo(InvitationStatus.PENDING);
    verify(invitations).saveAndFlush(any(MembershipInvitation.class));
  }

  @Test
  void creatingInvitationRejectsAlreadyActiveMember() {
    UUID polityId = UUID.randomUUID();
    UUID inviterUserId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    Membership inviter = member(polityId, inviterUserId, UUID.randomUUID());
    Membership active = member(polityId, inviteeUserId, UUID.randomUUID());
    ConstitutionVersion constitution = constitution(polityId);
    ProvisionalUser invitee = new ProvisionalUser(inviteeUserId, "subject:invitee");

    when(membershipService.active(polityId, inviterUserId)).thenReturn(inviter);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(identityUsers.createProvisional("friend@example.com")).thenReturn(invitee);
    when(memberships.findEntityByPolityIdAndUserId(polityId, inviteeUserId))
        .thenReturn(Optional.of(active));

    assertThatThrownBy(
            () ->
                service.create(
                    polityId,
                    new AuthenticatedUser(inviterUserId, "subject:inviter", "Ada"),
                    new CreateMemberInvitationInput("Friend@Example.com")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This user is already a member.");

    verify(invitations, never()).saveAndFlush(any());
    verify(grants, never()).stage(any());
  }

  @Test
  void creatingInvitationDoesNotBypassMissingAdmissionPower() {
    UUID polityId = UUID.randomUUID();
    UUID inviterUserId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    Membership inviter = member(polityId, inviterUserId, inviterMembershipId);
    ConstitutionVersion constitution = constitution(polityId);

    when(membershipService.active(polityId, inviterUserId)).thenReturn(inviter);
    when(polities.constitution(polityId)).thenReturn(constitution);
    doThrow(
            ApiException.forbidden(
                "constitutional_power_missing",
                "The governing constitution does not authorize this action."))
        .when(authority)
        .require(inviter, constitution, PowerCode.ADMIT_MEMBER);

    assertThatThrownBy(
            () ->
                service.create(
                    polityId,
                    new AuthenticatedUser(inviterUserId, "subject:inviter", "Ada"),
                    new CreateMemberInvitationInput("Friend@Example.com")))
        .isInstanceOf(ApiException.class)
        .hasMessage("The governing constitution does not authorize this action.");

    verify(actionAvailability, never()).hasProvisionalFounderAdmissionAuthority(inviter);
    verify(identityUsers, never()).createProvisional(any());
  }

  @Test
  void acceptingInvitationCreatesMembershipAndStagesAccessGrant() {
    UUID polityId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    UUID admittedMembershipId = UUID.randomUUID();
    MembershipInvitation invitation =
        new MembershipInvitation(
            polityId,
            inviteeUserId,
            "subject:old-invitee",
            "friend@example.com",
            inviterMembershipId,
            NOW.minusDays(1));
    ReflectionTestUtils.setField(invitation, "id", invitationId);
    IdentityUser invitee =
        new IdentityUser(
            inviteeUserId,
            "subject:invitee",
            "friend@example.com",
            "Bea",
            null,
            IdentityUserStatus.ACTIVE,
            true,
            NOW.minusDays(10),
            NOW.minusDays(1));
    ConstitutionVersion constitution = constitution(polityId);
    Jurisdiction jurisdiction = jurisdiction(polityId);
    MembershipProjection memberProjection = memberProjection(admittedMembershipId, inviteeUserId);

    when(identityUsers.get(inviteeUserId)).thenReturn(invitee);
    when(invitations.findEntityByIdAndStatus(invitationId, InvitationStatus.PENDING))
        .thenReturn(Optional.of(invitation));
    when(memberships.saveAndFlush(any(Membership.class)))
        .thenAnswer(
            invocation -> {
              Membership member = invocation.getArgument(0);
              ReflectionTestUtils.setField(member, "id", admittedMembershipId);
              return member;
            });
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(memberships.findProjectedById(admittedMembershipId))
        .thenReturn(Optional.of(memberProjection));

    var result =
        service.accept(
            invitationId, new AuthenticatedUser(inviteeUserId, "subject:invitee", "Bea"));

    assertThat(result.id()).isEqualTo(admittedMembershipId);
    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    assertThat(invitation.getRespondedAt()).isEqualTo(NOW);
    ArgumentCaptor<Membership> memberCaptor = ArgumentCaptor.forClass(Membership.class);
    verify(memberships).saveAndFlush(memberCaptor.capture());
    assertThat(memberCaptor.getValue().getAdmittedBy()).isEqualTo(inviterMembershipId);
    verify(grants).stage(any());
    verify(polities).completeBootstrapIfReady(polityId, NOW);
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
  void acceptingInvitationReactivatesResignedMembership() {
    UUID polityId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    UUID inviterMembershipId = UUID.randomUUID();
    UUID admittedMembershipId = UUID.randomUUID();
    Membership resigned = member(polityId, inviteeUserId, admittedMembershipId);
    resigned.resign(NOW.minusDays(1));
    MembershipInvitation invitation =
        new MembershipInvitation(
            polityId,
            inviteeUserId,
            "subject:invitee",
            "friend@example.com",
            inviterMembershipId,
            NOW.minusDays(1));
    ReflectionTestUtils.setField(invitation, "id", invitationId);
    IdentityUser invitee =
        new IdentityUser(
            inviteeUserId,
            "subject:invitee",
            "friend@example.com",
            "Bea",
            null,
            IdentityUserStatus.ACTIVE,
            true,
            NOW.minusDays(10),
            NOW.minusDays(1));
    ConstitutionVersion constitution = constitution(polityId);
    Jurisdiction jurisdiction = jurisdiction(polityId);
    MembershipProjection memberProjection = memberProjection(admittedMembershipId, inviteeUserId);

    when(identityUsers.get(inviteeUserId)).thenReturn(invitee);
    when(invitations.findEntityByIdAndStatus(invitationId, InvitationStatus.PENDING))
        .thenReturn(Optional.of(invitation));
    when(memberships.findEntityByPolityIdAndUserId(polityId, inviteeUserId))
        .thenReturn(Optional.of(resigned));
    when(memberships.saveAndFlush(resigned)).thenReturn(resigned);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(memberships.findProjectedById(admittedMembershipId))
        .thenReturn(Optional.of(memberProjection));

    var result =
        service.accept(
            invitationId, new AuthenticatedUser(inviteeUserId, "subject:invitee", "Bea"));

    assertThat(result.id()).isEqualTo(admittedMembershipId);
    assertThat(resigned.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    assertThat(resigned.getAuthorizationSubject()).isEqualTo("subject:invitee");
    assertThat(resigned.getEmail()).isEqualTo("friend@example.com");
    assertThat(resigned.getDisplayName()).isEqualTo("Bea");
    assertThat(resigned.getAdmittedAt()).isEqualTo(NOW);
    assertThat(resigned.getAdmittedBy()).isEqualTo(inviterMembershipId);
    assertThat(resigned.getResignedAt()).isNull();
    verify(grants).stage(any());
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
  void acceptingInvitationRejectsAlreadyActiveMembership() {
    UUID polityId = UUID.randomUUID();
    UUID invitationId = UUID.randomUUID();
    UUID inviteeUserId = UUID.randomUUID();
    Membership active = member(polityId, inviteeUserId, UUID.randomUUID());
    MembershipInvitation invitation =
        new MembershipInvitation(
            polityId,
            inviteeUserId,
            "subject:invitee",
            "friend@example.com",
            UUID.randomUUID(),
            NOW.minusDays(1));
    IdentityUser invitee =
        new IdentityUser(
            inviteeUserId,
            "subject:invitee",
            "friend@example.com",
            "Bea",
            null,
            IdentityUserStatus.ACTIVE,
            true,
            NOW.minusDays(10),
            NOW.minusDays(1));

    when(identityUsers.get(inviteeUserId)).thenReturn(invitee);
    when(invitations.findEntityByIdAndStatus(invitationId, InvitationStatus.PENDING))
        .thenReturn(Optional.of(invitation));
    when(memberships.findEntityByPolityIdAndUserId(polityId, inviteeUserId))
        .thenReturn(Optional.of(active));

    assertThatThrownBy(
            () ->
                service.accept(
                    invitationId, new AuthenticatedUser(inviteeUserId, "subject:invitee", "Bea")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This user is already a member.");

    verify(memberships, never()).saveAndFlush(any());
    verify(grants, never()).stage(any());
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

  private MembershipInvitationProjection invitationProjection(UUID invitationId, UUID polityId) {
    MembershipInvitationProjection projection = mock(MembershipInvitationProjection.class);
    when(projection.getId()).thenReturn(invitationId);
    when(projection.getPolityId()).thenReturn(polityId);
    when(projection.getEmail()).thenReturn("friend@example.com");
    when(projection.getInvitedBy()).thenReturn(UUID.randomUUID());
    when(projection.getStatus()).thenReturn(InvitationStatus.PENDING);
    when(projection.getInvitedAt()).thenReturn(NOW);
    return projection;
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
