package com.odonta.polity.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.common.api.ApiException;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.GovernmentReadinessDiagnostic;
import com.odonta.polity.model.GovernmentReadinessStatus;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.ProcedureTemplateKey;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProposalProjection;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProjection;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.service.MembershipService;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PolityActionAvailabilityResolverTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final MembershipService membershipService = mock(MembershipService.class);
  private final ConstitutionalAuthority authority = mock(ConstitutionalAuthority.class);
  private final AppealProposalRepository appealProposals = mock(AppealProposalRepository.class);
  private final AppealRepository appeals = mock(AppealRepository.class);
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final MotionRepository motions = mock(MotionRepository.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private GovernmentAssessmentResolver governmentAssessments;
  private PolityActionAvailabilityResolver actionAvailability;

  @BeforeEach
  void setUp() {
    ProcedureElectorateResolver procedureElectorates =
        new ProcedureElectorateResolver(memberships, membershipService, officeTerms);
    governmentAssessments =
        new GovernmentAssessmentResolver(
            Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
            powers,
            memberships,
            membershipService,
            offices,
            officeTerms,
            procedureElectorates,
            procedures);
    actionAvailability =
        new PolityActionAvailabilityResolver(
            Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
            access,
            appealProposals,
            appeals,
            authority,
            constitutions,
            powers,
            governmentAssessments,
            memberships,
            motions,
            officeTerms,
            polities,
            procedureElectorates,
            procedures,
            sanctions);
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenReturn(true);
  }

  @Test
  void actionsExposeAvailabilityReasonCodes() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    stubPower(constitution, PowerCode.ADMIT_MEMBER, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_MOTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_ELECTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_SANCTION, PowerHolderScope.OFFICE, Office.TRIBUNE);
    stubPower(constitution, PowerCode.INTRODUCE_APPEAL, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution,
        PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW,
        PowerHolderScope.ACTIVE_MEMBER,
        null);
    stubPower(constitution, PowerCode.INTRODUCE_AMENDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_DISBANDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.REQUEST_CERTIFICATION, PowerHolderScope.ACTIVE_MEMBER, null);
    when(authority.allows(member, constitution, PowerCode.ADMIT_MEMBER)).thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_MOTION)).thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_OFFICE_ELECTION))
        .thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_SANCTION)).thenReturn(false);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_APPEAL)).thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW))
        .thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW))
        .thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_AMENDMENT)).thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_DISBANDMENT)).thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.REQUEST_CERTIFICATION)).thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(2L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member));
    when(membershipService.hasPoliticalStanding(member.getId(), NOW)).thenReturn(true);
    stubOfficeHolderProcedure(
        constitution, Procedure.ORDINARY_RESOLUTION, EffectType.ADOPT_RESOLUTION);
    stubActiveMemberProcedure(constitution, Procedure.DISBANDMENT, EffectType.DISBAND_POLITY);

    var result = actionAvailability.resolve(polityId, userId);

    assertThat(result.readiness().status()).isEqualTo(GovernmentReadinessStatus.PROVISIONAL);
    assertThat(result.readiness().diagnostics())
        .containsExactly(GovernmentReadinessDiagnostic.NEEDS_MORE_STANDING_MEMBERS);
    assertThat(result.inviteMembers().available()).isTrue();
    assertThat(result.introduceMotion().available()).isFalse();
    assertThat(result.introduceMotion().reason()).isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceSanction().available()).isFalse();
    assertThat(result.introduceSanction().reason()).isEqualTo("constitutional_office_vacant");
    assertThat(result.introduceDisbandment().available()).isTrue();
    assertThat(result.resignMembership().available()).isFalse();
    assertThat(result.resignMembership().reason())
        .isEqualTo("provisional_founder_resignation_unavailable");
  }

  @Test
  void actionAvailabilityChecksProcedureElectoratesForFullGovernmentProcedures() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    Membership second = member(polityId, UUID.randomUUID());
    Membership third = member(polityId, UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    stubPower(constitution, PowerCode.ADMIT_MEMBER, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_MOTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_ELECTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_SANCTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_APPEAL, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution,
        PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW,
        PowerHolderScope.ACTIVE_MEMBER,
        null);
    stubPower(constitution, PowerCode.INTRODUCE_AMENDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_DISBANDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.REQUEST_CERTIFICATION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubOfficeHolderProcedure(
        constitution, Procedure.ORDINARY_RESOLUTION, EffectType.ADOPT_RESOLUTION);
    stubOfficeHolderProcedure(constitution, Procedure.OFFICE_ELECTION, EffectType.ELECT_OFFICE);
    stubOfficeHolderProcedure(
        constitution, Procedure.CONSTITUTION_AMENDMENT, EffectType.AMEND_CONSTITUTION);
    stubActiveMemberProcedure(constitution, Procedure.APPEAL, EffectType.GRANT_APPEAL);
    stubActiveMemberProcedure(
        constitution, Procedure.OFFICE_TERM_REVIEW, EffectType.VACATE_OFFICE_TERM);
    stubActiveMemberProcedure(
        constitution, Procedure.CONSTITUTIONAL_REVIEW, EffectType.VOID_OFFICIAL_ACT);
    stubActiveMemberProcedure(constitution, Procedure.DISBANDMENT, EffectType.DISBAND_POLITY);
    when(authority.allows(
            any(Membership.class), any(ConstitutionVersion.class), any(PowerCode.class)))
        .thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member, second, third));
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenReturn(true);
    when(officeTerms.existsByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, Office.MAGISTRATE, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(false);

    var result = actionAvailability.resolve(polityId, userId);

    assertThat(result.introduceMotion().available()).isFalse();
    assertThat(result.introduceMotion().reason()).isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceOfficeElection().available()).isFalse();
    assertThat(result.introduceOfficeElection().reason())
        .isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceAmendment().available()).isFalse();
    assertThat(result.introduceAmendment().reason())
        .isEqualTo("procedure_electorate_office_vacant");
  }

  @Test
  void actionAvailabilityAllowsLastMemberResignationWhenDisbandmentNeedsTwoElectors() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Structured Charter", "Rules", NOW);
    ReflectionTestUtils.setField(constitution, "id", UUID.randomUUID());
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));
    Procedure disbandment =
        procedure(
            constitution,
            Procedure.DISBANDMENT,
            ProcedureElectorate.ACTIVE_MEMBERS,
            null,
            2,
            EffectType.DISBAND_POLITY);

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    stubPower(constitution, PowerCode.INTRODUCE_DISBANDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.DISBANDMENT))
        .thenReturn(Optional.of(disbandment));
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_DISBANDMENT)).thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(1L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member));
    when(membershipService.hasPoliticalStanding(member.getId(), NOW)).thenReturn(true);

    var result = actionAvailability.resolve(polityId, userId);

    assertThat(result.introduceDisbandment().available()).isFalse();
    assertThat(result.introduceDisbandment().reason())
        .isEqualTo("procedure_electorate_below_minimum");
    assertThat(result.resignMembership().available()).isTrue();
  }

  @Test
  void judicialActionsAreBlockedWhenMagistrateElectorateIsVacant() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    Membership second = member(polityId, UUID.randomUUID());
    Membership third = member(polityId, UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    stubPower(constitution, PowerCode.ADMIT_MEMBER, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_MOTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_ELECTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_SANCTION, PowerHolderScope.OFFICE, Office.TRIBUNE);
    stubPower(constitution, PowerCode.INTRODUCE_APPEAL, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution,
        PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW,
        PowerHolderScope.ACTIVE_MEMBER,
        null);
    stubPower(constitution, PowerCode.INTRODUCE_AMENDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_DISBANDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.REQUEST_CERTIFICATION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubJudicialProcedure(constitution, Procedure.APPEAL);
    stubJudicialProcedure(constitution, Procedure.OFFICE_TERM_REVIEW);
    stubJudicialProcedure(constitution, Procedure.CONSTITUTIONAL_REVIEW);
    stubActiveMemberProcedure(constitution, Procedure.DISBANDMENT, EffectType.DISBAND_POLITY);
    when(authority.allows(
            any(Membership.class), any(ConstitutionVersion.class), any(PowerCode.class)))
        .thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member, second, third));
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenReturn(true);
    when(officeTerms.existsByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, Office.MAGISTRATE, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(false);
    when(officeTerms.existsByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, Office.TRIBUNE, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(true);

    var result = actionAvailability.resolve(polityId, userId);

    assertThat(result.introduceSanction().available()).isFalse();
    assertThat(result.introduceSanction().reason()).isEqualTo("appeal_procedure_unavailable");
    assertThat(result.introduceAppeal().available()).isFalse();
    assertThat(result.introduceAppeal().reason()).isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceOfficeTermReview().available()).isFalse();
    assertThat(result.introduceOfficeTermReview().reason())
        .isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceConstitutionalReview().available()).isFalse();
    assertThat(result.introduceConstitutionalReview().reason())
        .isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceDisbandment().available()).isTrue();
    assertThat(result.resignMembership().available()).isTrue();
  }

  @Test
  void suspendedMembersCanSeeOwnAppealAvailability() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    Membership sanctionIntroducer = member(polityId, UUID.randomUUID());
    Membership judge = member(polityId, UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));
    UUID sanctionMotionId = UUID.randomUUID();
    Sanction sanction =
        new Sanction(
            polityId,
            sanctionMotionId,
            member.getId(),
            SanctionType.SUSPENSION,
            "Timeout",
            NOW.minusDays(1),
            NOW.plusDays(3));
    ReflectionTestUtils.setField(sanction, "id", UUID.randomUUID());

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    stubAllActiveMemberPowers(constitution);
    stubActiveMemberProcedure(constitution, Procedure.APPEAL, EffectType.GRANT_APPEAL);
    when(authority.allows(
            any(Membership.class), any(ConstitutionVersion.class), any(PowerCode.class)))
        .thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_APPEAL))
        .thenThrow(ApiException.forbidden("political_standing_required", "Standing required."));
    when(authority.allowsOwnAppealIntroduction(member, constitution)).thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member, sanctionIntroducer, judge));
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenAnswer(invocation -> !invocation.getArgument(0).equals(member.getId()));
    when(sanctions
            .findProjectionsByPolityIdAndTargetMembershipIdAndStatusAndEndsAtAfterOrderByStartedAtDesc(
                polityId, member.getId(), SanctionStatus.ACTIVE, NOW))
        .thenReturn(List.of(projection(SanctionProjection.class, sanction)));
    when(appealProposals.findProjectionsByPolityIdAndSanctionId(polityId, sanction.getId()))
        .thenReturn(List.of());
    when(motions.findEntityByIdAndPolityId(sanctionMotionId, polityId))
        .thenReturn(
            Optional.of(
                motion(
                    polityId,
                    sanctionMotionId,
                    constitution.getId(),
                    sanctionIntroducer.getId(),
                    NOW.minusHours(1))));

    var result = actionAvailability.resolve(polityId, userId);

    assertThat(result.introduceAppeal().available()).isTrue();
  }

  @Test
  void suspendedMembersCanSeeOwnAppealCertificationAvailability() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));
    UUID appealMotionId = UUID.randomUUID();
    AppealProposal proposal =
        new AppealProposal(polityId, appealMotionId, UUID.randomUUID(), member.getId(), "Evidence");

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(constitutions.findEntityById(constitution.getId())).thenReturn(Optional.of(constitution));
    stubAllActiveMemberPowers(constitution);
    when(authority.allows(
            any(Membership.class), any(ConstitutionVersion.class), any(PowerCode.class)))
        .thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.REQUEST_CERTIFICATION))
        .thenThrow(ApiException.forbidden("political_standing_required", "Standing required."));
    when(authority.allowsAppealCertification(member, constitution)).thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member));
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenReturn(false);
    when(appealProposals.findProjectionsByPolityIdAndAppellantMembershipId(
            polityId, member.getId()))
        .thenReturn(List.of(projection(AppealProposalProjection.class, proposal)));
    when(motions.findEntityByIdAndPolityId(appealMotionId, polityId))
        .thenReturn(
            Optional.of(
                motion(
                    polityId,
                    appealMotionId,
                    constitution.getId(),
                    member.getId(),
                    NOW.minusMinutes(1))));

    var result = actionAvailability.resolve(polityId, userId);

    assertThat(result.requestCertification().available()).isTrue();
  }

  @Test
  void disbandmentRemainsAvailableWhenThreeActiveMembersHaveProvisionalStanding() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    Membership second = member(polityId, UUID.randomUUID());
    Membership suspended = member(polityId, UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    stubPower(constitution, PowerCode.ADMIT_MEMBER, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_MOTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_ELECTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_SANCTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_APPEAL, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution,
        PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW,
        PowerHolderScope.ACTIVE_MEMBER,
        null);
    stubPower(constitution, PowerCode.INTRODUCE_AMENDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_DISBANDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.REQUEST_CERTIFICATION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubActiveMemberProcedure(constitution, Procedure.DISBANDMENT, EffectType.DISBAND_POLITY);
    when(authority.allows(
            any(Membership.class), any(ConstitutionVersion.class), any(PowerCode.class)))
        .thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member, second, suspended));
    when(membershipService.hasPoliticalStanding(member.getId(), NOW)).thenReturn(true);
    when(membershipService.hasPoliticalStanding(second.getId(), NOW)).thenReturn(true);
    when(membershipService.hasPoliticalStanding(suspended.getId(), NOW)).thenReturn(false);
    stubOfficeHolderProcedure(
        constitution, Procedure.ORDINARY_RESOLUTION, EffectType.ADOPT_RESOLUTION);

    var result = actionAvailability.resolve(polityId, userId);

    assertThat(result.introduceMotion().available()).isFalse();
    assertThat(result.introduceMotion().reason()).isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceDisbandment().available()).isTrue();
  }

  @Test
  void provisionalAdmissionAuthorityBelongsOnlyToFounderBeforeFullGovernment() {
    UUID polityId = UUID.randomUUID();
    UUID founderUserId = UUID.randomUUID();
    Membership founder = member(polityId, founderUserId);
    Membership citizen = member(polityId, UUID.randomUUID());
    Polity polity = polity(polityId, founderUserId);

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(founder, citizen));
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenReturn(true);
    assertThat(actionAvailability.hasProvisionalFounderAdmissionAuthority(founder)).isTrue();
    assertThat(actionAvailability.hasProvisionalFounderAdmissionAuthority(citizen)).isFalse();
  }

  @Test
  void provisionalAdmissionAuthoritySurvivesExpiredBootstrapStewardTerm() {
    UUID polityId = UUID.randomUUID();
    UUID founderUserId = UUID.randomUUID();
    Membership founder = member(polityId, founderUserId);
    Membership citizen = member(polityId, UUID.randomUUID());
    Polity polity = polity(polityId, founderUserId);

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(founder, citizen));
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenReturn(true);

    assertThat(actionAvailability.hasProvisionalFounderAdmissionAuthority(founder)).isTrue();
  }

  @Test
  void provisionalAdmissionAuthorityEndsAtFullGovernmentSize() {
    UUID polityId = UUID.randomUUID();
    UUID founderUserId = UUID.randomUUID();
    Membership founder = member(polityId, founderUserId);
    Polity polity = polity(polityId, founderUserId);
    Membership second = member(polityId, UUID.randomUUID());
    Membership third = member(polityId, UUID.randomUUID());

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(founder, second, third));
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenReturn(true);

    assertThat(actionAvailability.hasProvisionalFounderAdmissionAuthority(founder)).isFalse();
  }

  @Test
  void provisionalAdmissionAuthorityDoesNotReturnAfterBootstrapCompletes() {
    UUID polityId = UUID.randomUUID();
    UUID founderUserId = UUID.randomUUID();
    Membership founder = member(polityId, founderUserId);
    Polity polity = polity(polityId, founderUserId);
    polity.completeBootstrap(NOW.minusDays(1));

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));

    assertThat(actionAvailability.hasProvisionalFounderAdmissionAuthority(founder)).isFalse();
  }

  private Membership member(UUID polityId, UUID userId) {
    Membership member =
        new Membership(
            polityId, userId, "subject:" + userId, "founder@example.com", "Founder", NOW, null);
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private Polity polity(UUID polityId, UUID founderUserId) {
    Polity polity = new Polity("Pocket Republic", PolityVisibility.PRIVATE, founderUserId);
    ReflectionTestUtils.setField(polity, "id", polityId);
    return polity;
  }

  private void stubPower(
      ConstitutionVersion constitution,
      PowerCode code,
      PowerHolderScope holderScope,
      String holderOfficeCode) {
    ConstitutionalPower power =
        holderScope == PowerHolderScope.OFFICE
            ? new ConstitutionalPower(
                constitution.getPolityId(),
                constitution.getId(),
                code,
                code.name(),
                holderOfficeCode)
            : new ConstitutionalPower(
                constitution.getPolityId(), constitution.getId(), code, code.name(), holderScope);
    when(powers.findEntityByConstitutionVersionIdAndCode(constitution.getId(), code))
        .thenReturn(Optional.of(power));
  }

  private void stubAllActiveMemberPowers(ConstitutionVersion constitution) {
    stubPower(constitution, PowerCode.ADMIT_MEMBER, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_MOTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_ELECTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_SANCTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_APPEAL, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution,
        PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW,
        PowerHolderScope.ACTIVE_MEMBER,
        null);
    stubPower(constitution, PowerCode.INTRODUCE_AMENDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_DISBANDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.REQUEST_CERTIFICATION, PowerHolderScope.ACTIVE_MEMBER, null);
  }

  private void stubJudicialProcedure(ConstitutionVersion constitution, String procedureCode) {
    stubOfficeHolderProcedure(constitution, procedureCode, EffectType.GRANT_APPEAL);
  }

  private void stubOfficeHolderProcedure(
      ConstitutionVersion constitution, String procedureCode, EffectType effectType) {
    Procedure procedure =
        procedure(
            constitution,
            procedureCode,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            2,
            effectType);
    when(procedures.findEntityByConstitutionVersionIdAndCode(constitution.getId(), procedureCode))
        .thenReturn(Optional.of(procedure));
  }

  private void stubActiveMemberProcedure(
      ConstitutionVersion constitution, String procedureCode, EffectType effectType) {
    Procedure procedure =
        procedure(
            constitution, procedureCode, ProcedureElectorate.ACTIVE_MEMBERS, null, 1, effectType);
    when(procedures.findEntityByConstitutionVersionIdAndCode(constitution.getId(), procedureCode))
        .thenReturn(Optional.of(procedure));
  }

  private Procedure procedure(
      ConstitutionVersion constitution,
      String procedureCode,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      int minimumElectorCount,
      EffectType effectType) {
    return new Procedure(
        constitution.getPolityId(),
        constitution.getId(),
        UUID.randomUUID(),
        procedureCode,
        procedureCode,
        (ProcedureTemplateKey) null,
        1,
        2,
        VotingThreshold.SIMPLE_MAJORITY_CAST,
        electorate,
        electorateOfficeCode,
        minimumElectorCount,
        0,
        24,
        effectType);
  }

  private Motion motion(
      UUID polityId,
      UUID motionId,
      UUID constitutionVersionId,
      UUID introducedBy,
      OffsetDateTime certificationOpensAt) {
    Motion motion =
        new Motion(
            polityId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            constitutionVersionId,
            UUID.randomUUID(),
            introducedBy,
            "Appeal",
            "Review the sanction.",
            EffectType.GRANT_APPEAL,
            NOW.minusDays(2),
            NOW.minusDays(1),
            NOW.minusHours(1),
            certificationOpensAt);
    ReflectionTestUtils.setField(motion, "id", motionId);
    return motion;
  }

  private static <T> T projection(Class<T> type, Object source) {
    Object proxy =
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (ignored, method, args) -> {
              if (method.getDeclaringClass() == Object.class) {
                return method.invoke(source, args);
              }
              return source.getClass().getMethod(method.getName()).invoke(source);
            });
    return type.cast(proxy);
  }
}
