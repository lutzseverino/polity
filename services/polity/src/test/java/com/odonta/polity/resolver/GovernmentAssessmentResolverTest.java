package com.odonta.polity.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalHealthDiagnostic;
import com.odonta.polity.model.ConstitutionalHealthStatus;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.GovernmentReadinessDiagnostic;
import com.odonta.polity.model.GovernmentReadinessStatus;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.result.ActionUnavailableReason;
import com.odonta.polity.result.GovernmentAssessmentResult;
import com.odonta.polity.service.MembershipService;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GovernmentAssessmentResolverTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-01T10:00:00Z");

  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final MembershipService membershipService = mock(MembershipService.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final GovernmentAssessmentResolver resolver =
      new GovernmentAssessmentResolver(
          Clock.fixed(Instant.from(NOW), ZoneOffset.UTC),
          powers,
          memberships,
          membershipService,
          offices,
          officeTerms,
          new ProcedureElectorateResolver(memberships, membershipService, officeTerms),
          procedures);

  @Test
  void oneStandingMemberIsProvisionalWithoutDegradingAHealthyConstitution() {
    UUID polityId = UUID.randomUUID();
    ConstitutionVersion constitution = constitution(polityId);
    Polity polity = polity(polityId);

    stubStandardConstitution(constitution);
    stubStandingMembers(polityId, 1);

    GovernmentAssessmentResult result = resolver.assess(polity, constitution);

    assertThat(result.readiness().status()).isEqualTo(GovernmentReadinessStatus.PROVISIONAL);
    assertThat(result.readiness().diagnostics())
        .containsExactly(GovernmentReadinessDiagnostic.NEEDS_MORE_STANDING_MEMBERS);
    assertThat(result.constitutionalHealth().status())
        .isEqualTo(ConstitutionalHealthStatus.HEALTHY);
  }

  @Test
  void twoStandingMembersCanUseTwoElectorProceduresWhileRemainingProvisional() {
    UUID polityId = UUID.randomUUID();
    ConstitutionVersion constitution = constitution(polityId);
    Polity polity = polity(polityId);

    stubStandardConstitution(constitution);
    stubStandingMembers(polityId, 2);

    GovernmentAssessmentResult result = resolver.assess(polity, constitution);

    assertThat(result.readiness().status()).isEqualTo(GovernmentReadinessStatus.PROVISIONAL);
    assertThat(
            resolver
                .procedureAvailability(polityId, constitution, Procedure.OFFICE_ELECTION)
                .available())
        .isTrue();
  }

  @Test
  void procedureAvailabilityRequiresMinimumOfficeHolderElectorate() {
    UUID polityId = UUID.randomUUID();
    ConstitutionVersion constitution = constitution(polityId);
    Membership magistrate = member(polityId, "Magistrate");
    Procedure procedure =
        new Procedure(
            polityId,
            constitution.getId(),
            UUID.randomUUID(),
            Procedure.CONSTITUTIONAL_REVIEW,
            "Constitutional review",
            null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            2,
            0,
            24,
            EffectType.VOID_OFFICIAL_ACT,
            null);
    OfficeTerm term =
        new OfficeTerm(
            polityId,
            UUID.randomUUID(),
            Office.MAGISTRATE,
            magistrate.getId(),
            NOW.minusDays(1),
            NOW.plusDays(14));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTIONAL_REVIEW))
        .thenReturn(Optional.of(procedure));
    when(officeTerms.findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
            polityId, Office.MAGISTRATE, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(List.of(term));
    when(memberships.findEntityById(magistrate.getId())).thenReturn(Optional.of(magistrate));
    when(membershipService.hasPoliticalStanding(magistrate.getId(), NOW)).thenReturn(true);

    var result =
        resolver.procedureAvailability(polityId, constitution, Procedure.CONSTITUTIONAL_REVIEW);

    assertThat(result.available()).isFalse();
    assertThat(result.reason())
        .isEqualTo(ActionUnavailableReason.PROCEDURE_ELECTORATE_BELOW_MINIMUM);
  }

  @Test
  void fullSizePolityWithEmptyRepresentativeOfficesIsForming() {
    UUID polityId = UUID.randomUUID();
    ConstitutionVersion constitution = constitution(polityId);
    Polity polity = polity(polityId);

    stubStandardConstitution(constitution);
    stubStandingMembers(polityId, 3);

    GovernmentAssessmentResult result = resolver.assess(polity, constitution);

    assertThat(result.readiness().status()).isEqualTo(GovernmentReadinessStatus.FORMING_OFFICES);
    assertThat(result.readiness().diagnostics())
        .contains(
            GovernmentReadinessDiagnostic.ORDINARY_GOVERNANCE_ELECTORATE_UNAVAILABLE,
            GovernmentReadinessDiagnostic.APPEAL_ELECTORATE_UNAVAILABLE,
            GovernmentReadinessDiagnostic.CONSTITUTIONAL_REVIEW_ELECTORATE_UNAVAILABLE);
    assertThat(result.constitutionalHealth().status())
        .isEqualTo(ConstitutionalHealthStatus.HEALTHY);
  }

  @Test
  void staffedCouncilWithoutCourtLeavesJudicialReadinessDiagnostics() {
    UUID polityId = UUID.randomUUID();
    ConstitutionVersion constitution = constitution(polityId);
    Polity polity = polity(polityId);
    List<Membership> members = stubStandingMembers(polityId, 3);

    stubStandardConstitution(constitution);
    stubOfficeHolders(polityId, Office.COUNCILOR, members.subList(0, 1));

    GovernmentAssessmentResult result = resolver.assess(polity, constitution);

    assertThat(result.readiness().status()).isEqualTo(GovernmentReadinessStatus.FORMING_OFFICES);
    assertThat(result.readiness().diagnostics())
        .doesNotContain(GovernmentReadinessDiagnostic.ORDINARY_GOVERNANCE_ELECTORATE_UNAVAILABLE)
        .contains(
            GovernmentReadinessDiagnostic.APPEAL_ELECTORATE_UNAVAILABLE,
            GovernmentReadinessDiagnostic.CONSTITUTIONAL_REVIEW_ELECTORATE_UNAVAILABLE);
  }

  @Test
  void staffedCouncilAndCourtAreReadyAndHealthy() {
    UUID polityId = UUID.randomUUID();
    ConstitutionVersion constitution = constitution(polityId);
    Polity polity = polity(polityId);
    List<Membership> members = stubStandingMembers(polityId, 3);

    stubStandardConstitution(constitution);
    stubOfficeHolders(polityId, Office.COUNCILOR, members.subList(0, 1));
    stubOfficeHolders(polityId, Office.MAGISTRATE, members.subList(1, 3));

    GovernmentAssessmentResult result = resolver.assess(polity, constitution);

    assertThat(result.readiness().status()).isEqualTo(GovernmentReadinessStatus.READY);
    assertThat(result.readiness().diagnostics()).isEmpty();
    assertThat(result.constitutionalHealth().status())
        .isEqualTo(ConstitutionalHealthStatus.HEALTHY);
  }

  @Test
  void missingAppealPathDegradesHealthWithoutBecomingCritical() {
    UUID polityId = UUID.randomUUID();
    ConstitutionVersion constitution = constitution(polityId);
    Polity polity = polity(polityId);

    stubStandardConstitution(constitution);
    stubMissingProcedure(constitution, Procedure.APPEAL);
    stubStandingMembers(polityId, 3);

    GovernmentAssessmentResult result = resolver.assess(polity, constitution);

    assertThat(result.constitutionalHealth().status())
        .isEqualTo(ConstitutionalHealthStatus.DEGRADED);
    assertThat(result.constitutionalHealth().diagnostics())
        .contains(ConstitutionalHealthDiagnostic.APPEAL_PATH_UNAVAILABLE);
  }

  @Test
  void disbandedPolityReportsClosedReadinessAndCriticalHealth() {
    UUID polityId = UUID.randomUUID();
    ConstitutionVersion constitution = constitution(polityId);
    Polity polity = polity(polityId);
    polity.disband(NOW);

    GovernmentAssessmentResult result = resolver.assess(polity, constitution);

    assertThat(result.readiness().status()).isEqualTo(GovernmentReadinessStatus.DISBANDED);
    assertThat(result.readiness().diagnostics())
        .containsExactly(GovernmentReadinessDiagnostic.POLITY_DISBANDED);
    assertThat(result.constitutionalHealth().status())
        .isEqualTo(ConstitutionalHealthStatus.CRITICAL);
    assertThat(result.constitutionalHealth().diagnostics())
        .containsExactly(ConstitutionalHealthDiagnostic.POLITY_DISBANDED);
  }

  private void stubStandardConstitution(ConstitutionVersion constitution) {
    stubOffice(constitution, Office.COUNCILOR, 5);
    stubOffice(constitution, Office.MAGISTRATE, 3);
    stubOffice(constitution, Office.STEWARD, 1);
    stubOffice(constitution, Office.TRIBUNE, 1);
    stubPower(constitution, PowerCode.INTRODUCE_MOTION);
    stubPower(constitution, PowerCode.INTRODUCE_OFFICE_ELECTION);
    stubOfficePower(constitution, PowerCode.INTRODUCE_SANCTION, Office.TRIBUNE);
    stubPower(constitution, PowerCode.INTRODUCE_APPEAL);
    stubPower(constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW);
    stubPower(constitution, PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW);
    stubPower(constitution, PowerCode.INTRODUCE_AMENDMENT);
    stubPower(constitution, PowerCode.INTRODUCE_DISBANDMENT);
    stubOfficePower(constitution, PowerCode.ADMIT_MEMBER, Office.STEWARD);
    stubPower(constitution, PowerCode.REQUEST_CERTIFICATION);
    stubOfficeHolderProcedure(
        constitution,
        Procedure.ORDINARY_RESOLUTION,
        Office.COUNCILOR,
        1,
        EffectType.ADOPT_RESOLUTION);
    stubActiveMemberProcedure(constitution, Procedure.OFFICE_ELECTION, 2, EffectType.ELECT_OFFICE);
    stubActiveMemberProcedure(constitution, Procedure.SANCTION, 2, EffectType.APPLY_SANCTION);
    stubOfficeHolderProcedure(
        constitution, Procedure.APPEAL, Office.MAGISTRATE, 2, EffectType.GRANT_APPEAL);
    stubOfficeHolderProcedure(
        constitution,
        Procedure.OFFICE_TERM_REVIEW,
        Office.MAGISTRATE,
        2,
        EffectType.VACATE_OFFICE_TERM);
    stubOfficeHolderProcedure(
        constitution,
        Procedure.CONSTITUTIONAL_REVIEW,
        Office.MAGISTRATE,
        2,
        EffectType.VOID_OFFICIAL_ACT);
    stubActiveMemberProcedure(
        constitution, Procedure.CONSTITUTION_AMENDMENT, 2, EffectType.AMEND_CONSTITUTION);
    stubActiveMemberProcedure(constitution, Procedure.DISBANDMENT, 2, EffectType.DISBAND_POLITY);
  }

  private void stubPower(ConstitutionVersion constitution, PowerCode code) {
    when(powers.findEntityByConstitutionVersionIdAndCode(constitution.getId(), code))
        .thenReturn(
            Optional.of(
                new ConstitutionalPower(
                    constitution.getPolityId(),
                    constitution.getId(),
                    code,
                    code.name(),
                    PowerHolderScope.ACTIVE_MEMBER)));
  }

  private void stubOfficePower(
      ConstitutionVersion constitution, PowerCode code, String officeCode) {
    when(powers.findEntityByConstitutionVersionIdAndCode(constitution.getId(), code))
        .thenReturn(
            Optional.of(
                new ConstitutionalPower(
                    constitution.getPolityId(),
                    constitution.getId(),
                    code,
                    code.name(),
                    officeCode)));
  }

  private void stubOffice(ConstitutionVersion constitution, String code, int seatCount) {
    Office office =
        new Office(
            constitution.getPolityId(),
            constitution.getId(),
            UUID.randomUUID(),
            code,
            code,
            code,
            30,
            seatCount);
    when(offices.existsByConstitutionVersionIdAndCode(constitution.getId(), code)).thenReturn(true);
    when(offices.findEntityByConstitutionVersionIdAndCode(constitution.getId(), code))
        .thenReturn(Optional.of(office));
  }

  private void stubActiveMemberProcedure(
      ConstitutionVersion constitution, String code, int minimumElectors, EffectType effectType) {
    stubProcedure(
        constitution,
        new Procedure(
            constitution.getPolityId(),
            constitution.getId(),
            UUID.randomUUID(),
            code,
            code,
            null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.ACTIVE_MEMBERS,
            null,
            minimumElectors,
            0,
            24,
            effectType,
            null));
  }

  private void stubOfficeHolderProcedure(
      ConstitutionVersion constitution,
      String code,
      String officeCode,
      int minimumElectors,
      EffectType effectType) {
    stubProcedure(
        constitution,
        new Procedure(
            constitution.getPolityId(),
            constitution.getId(),
            UUID.randomUUID(),
            code,
            code,
            null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.OFFICE_HOLDERS,
            officeCode,
            minimumElectors,
            0,
            24,
            effectType,
            null));
  }

  private void stubProcedure(ConstitutionVersion constitution, Procedure procedure) {
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), procedure.getCode()))
        .thenReturn(Optional.of(procedure));
  }

  private void stubMissingProcedure(ConstitutionVersion constitution, String code) {
    when(procedures.findEntityByConstitutionVersionIdAndCode(constitution.getId(), code))
        .thenReturn(Optional.empty());
  }

  private List<Membership> stubStandingMembers(UUID polityId, int count) {
    List<Membership> members = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      Membership member = member(polityId, "Citizen " + (index + 1));
      members.add(member);
      when(membershipService.hasPoliticalStanding(member.getId(), NOW)).thenReturn(true);
      when(memberships.findEntityById(member.getId())).thenReturn(Optional.of(member));
    }
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(members);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE))
        .thenReturn((long) count);
    return members;
  }

  private void stubOfficeHolders(UUID polityId, String officeCode, List<Membership> members) {
    List<OfficeTerm> terms =
        members.stream()
            .map(
                member ->
                    new OfficeTerm(
                        polityId,
                        UUID.randomUUID(),
                        officeCode,
                        member.getId(),
                        NOW.minusDays(1),
                        NOW.plusDays(30)))
            .toList();
    when(officeTerms.findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
            polityId, officeCode, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(terms);
  }

  private ConstitutionVersion constitution(UUID polityId) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW.minusDays(10));
    ReflectionTestUtils.setField(constitution, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(constitution, "status", ConstitutionStatus.RATIFIED);
    return constitution;
  }

  private Polity polity(UUID polityId) {
    Polity polity = new Polity("Commons", PolityVisibility.PUBLIC, UUID.randomUUID());
    ReflectionTestUtils.setField(polity, "id", polityId);
    return polity;
  }

  private Membership member(UUID polityId, String displayName) {
    Membership member =
        new Membership(
            polityId,
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            displayName.toLowerCase().replace(" ", ".") + "@example.test",
            displayName,
            NOW.minusDays(2),
            null);
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }
}
