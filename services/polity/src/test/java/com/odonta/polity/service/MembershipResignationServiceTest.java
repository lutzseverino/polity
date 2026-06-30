package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.authorization.grant.RevocationPlan;
import com.odonta.authorization.grant.Revocations;
import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import com.odonta.polity.authorization.PolityRevocationPlanner;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.resolver.GovernmentAssessmentResolver;
import com.odonta.polity.resolver.ProcedureElectorateResolver;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

class MembershipResignationServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-29T04:00:00Z");

  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final JurisdictionRepository jurisdictions = mock(JurisdictionRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final MembershipService membershipService = mock(MembershipService.class);
  private final PolityRevocationPlanner revocationPlanner = new PolityRevocationPlanner();
  private final Revocations revocations = mock(Revocations.class);

  private final MembershipResignationService service =
      new MembershipResignationService(
          Clock.fixed(Instant.from(NOW), ZoneOffset.UTC),
          constitutions,
          new GovernmentAssessmentResolver(
              Clock.fixed(Instant.from(NOW), ZoneOffset.UTC),
              powers,
              memberships,
              membershipService,
              offices,
              officeTerms,
              new ProcedureElectorateResolver(memberships, membershipService, officeTerms),
              procedures),
          jurisdictions,
          memberships,
          officeTerms,
          officialRecords,
          polities,
          revocationPlanner,
          revocations);

  @Test
  void resigningMembershipRevokesAccessEndsOfficeTermsAndRecordsExit() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Membership member = member(polityId, userId, memberId);
    Polity polity = polity(polityId);
    ConstitutionVersion constitution = constitution(polityId);
    Jurisdiction jurisdiction = jurisdiction(polityId);
    OfficeTerm term =
        new OfficeTerm(
            polityId,
            UUID.randomUUID(),
            Office.STEWARD,
            memberId,
            NOW.minusDays(2),
            NOW.plusDays(5));

    when(polities.findEntityByIdForUpdate(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(2L);
    when(officeTerms.findEntitiesByPolityIdAndMembershipIdAndStatus(
            polityId, memberId, OfficeTermStatus.ACTIVE))
        .thenReturn(List.of(term));
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));

    service.resign(polityId, new AuthenticatedUser(userId, "subject:member", "Bea"));

    assertThat(member.getStatus()).isEqualTo(MembershipStatus.RESIGNED);
    assertThat(member.getResignedAt()).isEqualTo(NOW);
    assertThat(term.getStatus()).isEqualTo(OfficeTermStatus.ENDED);
    assertThat(term.getEndedAt()).isEqualTo(NOW);
    verify(memberships).saveAndFlush(member);
    InOrder resignationOrder = inOrder(polities, memberships);
    resignationOrder.verify(polities).findEntityByIdForUpdate(polityId);
    resignationOrder
        .verify(memberships)
        .countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE);
    verify(officeTerms).saveAllAndFlush(List.of(term));
    ArgumentCaptor<RevocationPlan> plan = ArgumentCaptor.forClass(RevocationPlan.class);
    verify(revocations).stage(plan.capture());
    assertThat(plan.getValue().resourceRevocations()).hasSize(1);
    assertThat(plan.getValue().resourceRevocations().getFirst().resourceServerClientId())
        .isEqualTo(PolityPermissions.CLIENT_ID);
    assertThat(plan.getValue().resourceRevocations().getFirst().resourceName())
        .isEqualTo(PolityResources.POLITY.resource(polityId).name());
    assertThat(plan.getValue().resourceRevocations().getFirst().actions())
        .containsExactly(PolityPermissions.READ, PolityPermissions.PARTICIPATE);
    verify(officialRecords)
        .append(
            eq(polityId),
            eq(jurisdiction.getId()),
            eq(constitution.getId()),
            eq(memberId),
            eq(OfficialRecordType.MEMBER_RESIGNED),
            eq(memberId),
            any(),
            any(),
            eq(NOW));
  }

  @Test
  void rejectsLastActiveMemberResignation() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId, UUID.randomUUID());

    when(polities.findEntityByIdForUpdate(polityId)).thenReturn(Optional.of(polity(polityId)));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    ConstitutionVersion constitution = constitution(polityId);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    stubDisbandmentAvailable(constitution, member);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(1L);

    assertThatThrownBy(
            () -> service.resign(polityId, new AuthenticatedUser(userId, "subject:member", "Bea")))
        .isInstanceOf(ApiException.class)
        .hasMessage("The last active citizen must disband the polity instead of resigning.");

    assertThat(member.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    verify(memberships, never()).saveAndFlush(any());
    verify(revocations, never()).stage(any());
    verify(officialRecords, never())
        .append(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void rejectsProvisionalFounderResignation() {
    UUID polityId = UUID.randomUUID();
    UUID founderUserId = UUID.randomUUID();
    Membership founder = member(polityId, founderUserId, UUID.randomUUID());

    when(polities.findEntityByIdForUpdate(polityId))
        .thenReturn(Optional.of(polity(polityId, founderUserId)));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, founderUserId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(founder));
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution(polityId)));
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(2L);

    assertThatThrownBy(
            () ->
                service.resign(
                    polityId, new AuthenticatedUser(founderUserId, "subject:founder", "Founder")))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "The founding citizen cannot resign before the polity reaches full government size.");

    assertThat(founder.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    verify(memberships, never()).saveAndFlush(any());
    verify(revocations, never()).stage(any());
  }

  @Test
  void lastActiveMemberResignationClosesPolityWhenDisbandmentIsUnavailable() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Membership member = member(polityId, userId, memberId);
    Polity polity = polity(polityId);
    polity.completeBootstrap(NOW.minusDays(1));
    ConstitutionVersion constitution = constitution(polityId);
    Jurisdiction jurisdiction = jurisdiction(polityId);

    when(polities.findEntityByIdForUpdate(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(1L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member));
    when(membershipService.hasPoliticalStanding(member.getId(), NOW)).thenReturn(false);
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));

    service.resign(polityId, new AuthenticatedUser(userId, "subject:member", "Bea"));

    assertThat(member.getStatus()).isEqualTo(MembershipStatus.RESIGNED);
    assertThat(polity.isDisbanded()).isTrue();
    verify(polities).saveAndFlush(polity);
    verify(officialRecords)
        .append(
            eq(polityId),
            eq(jurisdiction.getId()),
            eq(constitution.getId()),
            eq(memberId),
            eq(OfficialRecordType.POLITY_DISBANDED),
            eq(polityId),
            any(),
            any(),
            eq(NOW));
  }

  private Membership member(UUID polityId, UUID userId, UUID membershipId) {
    Membership member =
        new Membership(
            polityId, userId, "subject:member", "bea@example.com", "Bea", NOW.minusDays(10), null);
    ReflectionTestUtils.setField(member, "id", membershipId);
    return member;
  }

  private Polity polity(UUID polityId) {
    return polity(polityId, UUID.randomUUID());
  }

  private Polity polity(UUID polityId, UUID founderUserId) {
    Polity polity = new Polity("Friends", PolityVisibility.PUBLIC, founderUserId);
    ReflectionTestUtils.setField(polity, "id", polityId);
    return polity;
  }

  private ConstitutionVersion constitution(UUID polityId) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", "Body", NOW.minusDays(10));
    ReflectionTestUtils.setField(constitution, "id", UUID.randomUUID());
    return constitution;
  }

  private Jurisdiction jurisdiction(UUID polityId) {
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    return jurisdiction;
  }

  private void stubDisbandmentAvailable(ConstitutionVersion constitution, Membership member) {
    ConstitutionalPower power =
        new ConstitutionalPower(
            constitution.getPolityId(),
            constitution.getId(),
            PowerCode.INTRODUCE_DISBANDMENT,
            "Disband",
            PowerHolderScope.ACTIVE_MEMBER);
    Procedure procedure =
        new Procedure(
            constitution.getPolityId(),
            constitution.getId(),
            UUID.randomUUID(),
            Procedure.DISBANDMENT,
            "Disbandment",
            null,
            1,
            2,
            com.odonta.polity.model.VotingThreshold.TWO_THIRDS_ELIGIBLE,
            ProcedureElectorate.ACTIVE_MEMBERS,
            null,
            1,
            0,
            24,
            EffectType.DISBAND_POLITY);
    when(powers.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), PowerCode.INTRODUCE_DISBANDMENT))
        .thenReturn(Optional.of(power));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.DISBANDMENT))
        .thenReturn(Optional.of(procedure));
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            constitution.getPolityId(), MembershipStatus.ACTIVE))
        .thenReturn(List.of(member));
    when(membershipService.hasPoliticalStanding(member.getId(), NOW)).thenReturn(true);
  }
}
