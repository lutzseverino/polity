package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.common.api.ApiException;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionInstitutionChangeAction;
import com.odonta.polity.model.ConstitutionInstitutionChangeProposal;
import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import com.odonta.polity.model.ConstitutionPowerChangeProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.ConstitutionalReview;
import com.odonta.polity.model.ConstitutionalReviewProposal;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionBallot;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionProposal;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermReview;
import com.odonta.polity.model.OfficeTermReviewProposal;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.Resolution;
import com.odonta.polity.model.ResolutionStatus;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProposalProjection;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalProjection;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.ConstitutionalReviewProposalProjection;
import com.odonta.polity.repository.ConstitutionalReviewProposalRepository;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalProjection;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProposalProjection;
import com.odonta.polity.repository.OfficeTermReviewProposalRepository;
import com.odonta.polity.repository.OfficeTermReviewRepository;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class EffectApplicationServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-14T10:00:00Z");

  private final AppealProposalRepository appealProposals = mock(AppealProposalRepository.class);
  private final AppealRepository appeals = mock(AppealRepository.class);
  private final ConstitutionAmendmentProposalRepository amendmentProposals =
      mock(ConstitutionAmendmentProposalRepository.class);
  private final ConstitutionInstitutionChangeProposalRepository institutionChangeProposals =
      mock(ConstitutionInstitutionChangeProposalRepository.class);
  private final ConstitutionOfficeChangeProposalRepository officeChangeProposals =
      mock(ConstitutionOfficeChangeProposalRepository.class);
  private final ConstitutionPowerChangeProposalRepository powerChangeProposals =
      mock(ConstitutionPowerChangeProposalRepository.class);
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals =
      mock(ConstitutionProcedureChangeProposalRepository.class);
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final ConstitutionalReviewProposalRepository constitutionalReviewProposals =
      mock(ConstitutionalReviewProposalRepository.class);
  private final ConstitutionalReviewRepository constitutionalReviews =
      mock(ConstitutionalReviewRepository.class);
  private final InstitutionRepository institutions = mock(InstitutionRepository.class);
  private final JurisdictionRepository jurisdictions = mock(JurisdictionRepository.class);
  private final OfficeTermReviewProposalRepository officeTermReviewProposals =
      mock(OfficeTermReviewProposalRepository.class);
  private final OfficeTermReviewRepository officeTermReviews =
      mock(OfficeTermReviewRepository.class);
  private final MotionElectorRepository electors = mock(MotionElectorRepository.class);
  private final OfficeElectionBallotRepository officeElectionBallots =
      mock(OfficeElectionBallotRepository.class);
  private final OfficeElectionCandidateRepository officeElectionCandidates =
      mock(OfficeElectionCandidateRepository.class);
  private final OfficeElectionEvaluator officeElections = new OfficeElectionEvaluator();
  private final OfficeElectionProposalRepository officeElectionProposals =
      mock(OfficeElectionProposalRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final OfficialRecordRepository officialRecordEntries =
      mock(OfficialRecordRepository.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final ResolutionRepository resolutions = mock(ResolutionRepository.class);
  private final SanctionProposalRepository sanctionProposals =
      mock(SanctionProposalRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final MembershipService membershipService = mock(MembershipService.class);

  private final EffectApplicationService service =
      new EffectApplicationService(
          appealProposals,
          appeals,
          amendmentProposals,
          institutionChangeProposals,
          officeChangeProposals,
          powerChangeProposals,
          procedureChangeProposals,
          constitutions,
          powers,
          constitutionalReviewProposals,
          constitutionalReviews,
          institutions,
          jurisdictions,
          officeTermReviewProposals,
          officeTermReviews,
          electors,
          officeElectionBallots,
          officeElectionCandidates,
          officeElections,
          officeElectionProposals,
          offices,
          officeTerms,
          officialRecordEntries,
          polities,
          officialRecords,
          procedures,
          resolutions,
          sanctionProposals,
          sanctions,
          memberships,
          membershipService);

  @BeforeEach
  void setUp() {
    when(memberships.countByPolityIdAndStatus(
            any(), org.mockito.ArgumentMatchers.eq(MembershipStatus.ACTIVE)))
        .thenReturn(1L);
  }

  @Test
  void amendmentSupersedesCurrentConstitutionAndCopiesKernelRows() {
    UUID polityId = UUID.randomUUID();
    UUID currentConstitutionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    UUID procedureId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion current = constitution(polityId, currentConstitutionId, "Old body");
    Motion motion = motion(polityId, actor.getId(), currentConstitutionId);
    ConstitutionAmendmentProposal proposal =
        new ConstitutionAmendmentProposal(
            polityId,
            motion.getId(),
            "Second Constitution",
            "New body",
            "ordinary-resolution(threshold=TWO_THIRDS_CAST)");
    ReflectionTestUtils.setField(proposal, "id", UUID.randomUUID());
    ConstitutionProcedureChangeProposal procedureChange =
        new ConstitutionProcedureChangeProposal(
            polityId,
            proposal.getId(),
            Procedure.ORDINARY_RESOLUTION,
            null,
            null,
            null,
            VotingThreshold.TWO_THIRDS_CAST,
            null,
            null,
            null,
            null,
            null);
    Institution institution =
        new Institution(
            polityId,
            motion.getJurisdictionId(),
            currentConstitutionId,
            "Citizens' Assembly",
            InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(institution, "id", institutionId);
    Procedure procedure =
        new Procedure(
            polityId,
            currentConstitutionId,
            institutionId,
            Procedure.ORDINARY_RESOLUTION,
            "Ordinary resolution",
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            0,
            24,
            EffectType.ADOPT_RESOLUTION);
    ReflectionTestUtils.setField(procedure, "id", procedureId);

    when(amendmentProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionAmendmentProposalProjection.class, proposal)));
    when(procedureChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(
            List.of(
                projection(ConstitutionProcedureChangeProposalProjection.class, procedureChange)));
    when(institutionChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(officeChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(powerChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(constitutions.findEntityById(currentConstitutionId)).thenReturn(Optional.of(current));
    when(constitutions.saveAndFlush(any(ConstitutionVersion.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(institutions.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(institution));
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(procedures.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(procedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(currentConstitutionId))
        .thenReturn(List.of());
    when(powers.findEntitiesByConstitutionVersionId(currentConstitutionId)).thenReturn(List.of());

    service.apply(motion, actor, current, NOW);

    assertThat(current.getStatus()).isEqualTo(ConstitutionStatus.SUPERSEDED);
    ArgumentCaptor<ConstitutionVersion> constitutionCaptor =
        ArgumentCaptor.forClass(ConstitutionVersion.class);
    verify(constitutions, org.mockito.Mockito.times(2)).saveAndFlush(constitutionCaptor.capture());
    ConstitutionVersion amended = constitutionCaptor.getAllValues().get(1);
    assertThat(amended.getVersion()).isEqualTo(2);
    assertThat(amended.getTitle())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(2));
    assertThat(amended.getBody())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody());
    assertThat(amended.getTitleKey())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.titleKey());
    assertThat(amended.getBodyKey())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.bodyKey());
    ArgumentCaptor<Procedure> procedureCaptor = ArgumentCaptor.forClass(Procedure.class);
    verify(procedures).save(procedureCaptor.capture());
    assertThat(procedureCaptor.getValue().getThreshold())
        .isEqualTo(VotingThreshold.TWO_THIRDS_CAST);
    assertThat(procedureCaptor.getValue().getElectorate())
        .isEqualTo(ProcedureElectorate.ACTIVE_MEMBERS);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.CONSTITUTION_AMENDED),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void amendmentCreatesOfficeAndReassignsPowerInNewConstitution() {
    UUID polityId = UUID.randomUUID();
    UUID currentConstitutionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    UUID jurisdictionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion current = constitution(polityId, currentConstitutionId, "Old body");
    Jurisdiction jurisdiction = jurisdiction(polityId, jurisdictionId);
    Motion motion = motion(polityId, actor.getId(), currentConstitutionId);
    ConstitutionAmendmentProposal proposal =
        new ConstitutionAmendmentProposal(
            polityId,
            motion.getId(),
            "Second Constitution",
            "New body",
            "office:clerk(CREATE); power:ADMIT_MEMBER(OFFICE:clerk)");
    ReflectionTestUtils.setField(proposal, "id", UUID.randomUUID());
    Institution institution =
        new Institution(
            polityId,
            jurisdictionId,
            currentConstitutionId,
            "Citizens' Assembly",
            InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(institution, "id", institutionId);
    Office steward =
        new Office(
            polityId,
            currentConstitutionId,
            jurisdictionId,
            Office.STEWARD,
            "Steward",
            "Coordinates proceedings.",
            14);
    ConstitutionalPower admitMember =
        new ConstitutionalPower(
            polityId,
            currentConstitutionId,
            PowerCode.ADMIT_MEMBER,
            "Admit citizens",
            Office.STEWARD);
    ConstitutionOfficeChangeProposal officeChange =
        new ConstitutionOfficeChangeProposal(
            polityId,
            proposal.getId(),
            ConstitutionOfficeChangeAction.CREATE,
            "clerk",
            null,
            "Clerk",
            "Keeps the rolls.",
            30,
            1);
    ConstitutionPowerChangeProposal powerChange =
        new ConstitutionPowerChangeProposal(
            polityId, proposal.getId(), PowerCode.ADMIT_MEMBER, PowerHolderScope.OFFICE, "clerk");

    when(amendmentProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionAmendmentProposalProjection.class, proposal)));
    when(procedureChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(institutionChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(officeChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(
            List.of(projection(ConstitutionOfficeChangeProposalProjection.class, officeChange)));
    when(powerChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(
            List.of(projection(ConstitutionPowerChangeProposalProjection.class, powerChange)));
    when(constitutions.findEntityById(currentConstitutionId)).thenReturn(Optional.of(current));
    when(constitutions.saveAndFlush(any(ConstitutionVersion.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(institutions.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(institution));
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(procedures.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of());
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(currentConstitutionId))
        .thenReturn(List.of(steward));
    when(powers.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(admitMember));

    service.apply(motion, actor, current, NOW);

    ArgumentCaptor<Office> officeCaptor = ArgumentCaptor.forClass(Office.class);
    verify(offices, org.mockito.Mockito.times(2)).save(officeCaptor.capture());
    assertThat(officeCaptor.getAllValues())
        .extracting(Office::getCode)
        .containsExactlyInAnyOrder(Office.STEWARD, "clerk");
    ArgumentCaptor<ConstitutionalPower> powerCaptor =
        ArgumentCaptor.forClass(ConstitutionalPower.class);
    verify(powers).save(powerCaptor.capture());
    assertThat(powerCaptor.getValue().getHolderScope()).isEqualTo(PowerHolderScope.OFFICE);
    assertThat(powerCaptor.getValue().getHolderOfficeCode()).isEqualTo("clerk");
  }

  @Test
  void amendmentEvolvesInstitutionsAndMovesProceduresInNewConstitution() {
    UUID polityId = UUID.randomUUID();
    UUID currentConstitutionId = UUID.randomUUID();
    UUID assemblyId = UUID.randomUUID();
    UUID councilId = UUID.randomUUID();
    UUID courtId = UUID.randomUUID();
    UUID jurisdictionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion current = constitution(polityId, currentConstitutionId, "Old body");
    Jurisdiction jurisdiction = jurisdiction(polityId, jurisdictionId);
    Motion motion = motion(polityId, actor.getId(), currentConstitutionId);
    ConstitutionAmendmentProposal proposal =
        new ConstitutionAmendmentProposal(
            polityId,
            motion.getId(),
            "Second Constitution",
            "New body",
            "institution:%s(RETIRE); institution:%s(REVISE); institution:Audit Chamber(CREATE)"
                .formatted(assemblyId, courtId));
    ReflectionTestUtils.setField(proposal, "id", UUID.randomUUID());
    Institution assembly =
        new Institution(
            polityId,
            jurisdictionId,
            currentConstitutionId,
            "Citizens' Assembly",
            InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(assembly, "id", assemblyId);
    Institution court =
        new Institution(
            polityId,
            jurisdictionId,
            currentConstitutionId,
            "Magistrates' Court",
            InstitutionKind.JUDICIARY);
    ReflectionTestUtils.setField(court, "id", courtId);
    Institution council =
        new Institution(
            polityId,
            jurisdictionId,
            currentConstitutionId,
            "Citizens' Council",
            InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(council, "id", councilId);
    Procedure procedure =
        new Procedure(
            polityId,
            currentConstitutionId,
            assemblyId,
            Procedure.ORDINARY_RESOLUTION,
            "Ordinary resolution",
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            0,
            24,
            EffectType.ADOPT_RESOLUTION);
    ConstitutionInstitutionChangeProposal retireAssembly =
        new ConstitutionInstitutionChangeProposal(
            polityId,
            proposal.getId(),
            ConstitutionInstitutionChangeAction.RETIRE,
            assemblyId,
            null,
            null,
            null);
    ConstitutionInstitutionChangeProposal reviseCourt =
        new ConstitutionInstitutionChangeProposal(
            polityId,
            proposal.getId(),
            ConstitutionInstitutionChangeAction.REVISE,
            courtId,
            null,
            "High Court",
            null);
    ConstitutionInstitutionChangeProposal createAuditChamber =
        new ConstitutionInstitutionChangeProposal(
            polityId,
            proposal.getId(),
            ConstitutionInstitutionChangeAction.CREATE,
            null,
            jurisdictionId,
            "Audit Chamber",
            InstitutionKind.ASSEMBLY);
    ConstitutionProcedureChangeProposal moveProcedure =
        new ConstitutionProcedureChangeProposal(
            polityId,
            proposal.getId(),
            Procedure.ORDINARY_RESOLUTION,
            councilId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    when(amendmentProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionAmendmentProposalProjection.class, proposal)));
    when(institutionChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(
            List.of(
                projection(ConstitutionInstitutionChangeProposalProjection.class, retireAssembly),
                projection(ConstitutionInstitutionChangeProposalProjection.class, reviseCourt),
                projection(
                    ConstitutionInstitutionChangeProposalProjection.class, createAuditChamber)));
    when(procedureChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(
            List.of(
                projection(ConstitutionProcedureChangeProposalProjection.class, moveProcedure)));
    when(officeChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(powerChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(constitutions.findEntityById(currentConstitutionId)).thenReturn(Optional.of(current));
    when(constitutions.saveAndFlush(any(ConstitutionVersion.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(jurisdictions.findEntitiesByPolityId(polityId)).thenReturn(List.of(jurisdiction));
    when(institutions.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(assembly, court, council));
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(procedures.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(procedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(currentConstitutionId))
        .thenReturn(List.of());
    when(powers.findEntitiesByConstitutionVersionId(currentConstitutionId)).thenReturn(List.of());

    service.apply(motion, actor, current, NOW);

    ArgumentCaptor<ConstitutionVersion> constitutionCaptor =
        ArgumentCaptor.forClass(ConstitutionVersion.class);
    verify(constitutions, org.mockito.Mockito.times(2)).saveAndFlush(constitutionCaptor.capture());
    UUID amendedConstitutionId = constitutionCaptor.getAllValues().get(1).getId();
    ArgumentCaptor<Institution> institutionCaptor = ArgumentCaptor.forClass(Institution.class);
    verify(institutions, org.mockito.Mockito.times(3)).saveAndFlush(institutionCaptor.capture());
    assertThat(institutionCaptor.getAllValues())
        .extracting(Institution::getName)
        .containsExactlyInAnyOrder("High Court", "Citizens' Council", "Audit Chamber");
    assertThat(institutionCaptor.getAllValues())
        .allSatisfy(
            institution -> {
              assertThat(institution.getConstitutionVersionId()).isEqualTo(amendedConstitutionId);
              assertThat(institution.getJurisdictionId()).isEqualTo(jurisdictionId);
            });
    Institution copiedCouncil =
        institutionCaptor.getAllValues().stream()
            .filter(institution -> institution.getName().equals("Citizens' Council"))
            .findFirst()
            .orElseThrow();
    ArgumentCaptor<Procedure> procedureCaptor = ArgumentCaptor.forClass(Procedure.class);
    verify(procedures).save(procedureCaptor.capture());
    assertThat(procedureCaptor.getValue().getInstitutionId()).isEqualTo(copiedCouncil.getId());
  }

  @Test
  void officeElectionEffectAssignsTheWinningCandidate() {
    UUID polityId = UUID.randomUUID();
    UUID officeId = UUID.randomUUID();
    UUID winnerMembershipId = UUID.randomUUID();
    UUID otherCandidateMembershipId = UUID.randomUUID();
    Membership actor = member(polityId);
    Membership winner = member(polityId);
    ReflectionTestUtils.setField(winner, "id", winnerMembershipId);
    Membership otherCandidate = member(polityId);
    ReflectionTestUtils.setField(otherCandidate, "id", otherCandidateMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion = motion(polityId, actor.getId(), constitution.getId(), EffectType.ELECT_OFFICE);
    Office office =
        new Office(
            polityId,
            constitution.getId(),
            motion.getJurisdictionId(),
            Office.STEWARD,
            "Steward",
            "Coordinates.",
            14);
    ReflectionTestUtils.setField(office, "id", officeId);
    Procedure procedure =
        new Procedure(
            polityId,
            constitution.getId(),
            motion.getInstitutionId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            1,
            2,
            VotingThreshold.PLURALITY_CAST,
            0,
            24,
            EffectType.ELECT_OFFICE);
    ReflectionTestUtils.setField(procedure, "id", motion.getProcedureId());
    OfficeElectionProposal proposal =
        new OfficeElectionProposal(polityId, motion.getId(), officeId);
    OfficeElectionBallot first =
        new OfficeElectionBallot(polityId, motion.getId(), actor.getId(), winnerMembershipId, NOW);
    OfficeElectionBallot second =
        new OfficeElectionBallot(
            polityId, motion.getId(), UUID.randomUUID(), winnerMembershipId, NOW);

    when(officeElectionProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(Optional.of(projection(OfficeElectionProposalProjection.class, proposal)));
    when(offices.findEntityByIdAndPolityId(officeId, polityId)).thenReturn(Optional.of(office));
    when(procedures.findEntityById(motion.getProcedureId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motion.getId())).thenReturn(3L);
    when(officeElectionCandidates.findEntitiesByMotionIdAndStatus(
            motion.getId(), OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(
            List.of(
                new OfficeElectionCandidate(polityId, motion.getId(), winnerMembershipId),
                new OfficeElectionCandidate(polityId, motion.getId(), otherCandidateMembershipId)));
    when(officeElectionBallots.findEntitiesByMotionId(motion.getId()))
        .thenReturn(List.of(first, second));
    when(membershipService.get(winnerMembershipId)).thenReturn(winner);
    when(membershipService.get(otherCandidateMembershipId)).thenReturn(otherCandidate);
    when(membershipService.hasPoliticalStanding(winner, NOW)).thenReturn(true);
    when(membershipService.hasPoliticalStanding(otherCandidate, NOW)).thenReturn(true);
    when(officeTerms.saveAndFlush(any(OfficeTerm.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    service.apply(motion, actor, constitution, NOW);

    ArgumentCaptor<OfficeTerm> termCaptor = ArgumentCaptor.forClass(OfficeTerm.class);
    verify(officeTerms).saveAndFlush(termCaptor.capture());
    assertThat(termCaptor.getValue().getMembershipId()).isEqualTo(winnerMembershipId);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.OFFICE_ELECTED),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void officeElectionEffectRejectsFullOffice() {
    UUID polityId = UUID.randomUUID();
    UUID officeId = UUID.randomUUID();
    UUID winnerMembershipId = UUID.randomUUID();
    Membership actor = member(polityId);
    Membership winner = member(polityId);
    ReflectionTestUtils.setField(winner, "id", winnerMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion = motion(polityId, actor.getId(), constitution.getId(), EffectType.ELECT_OFFICE);
    Office office =
        new Office(
            polityId,
            constitution.getId(),
            motion.getJurisdictionId(),
            Office.STEWARD,
            "Steward",
            "Coordinates.",
            14);
    ReflectionTestUtils.setField(office, "id", officeId);
    Procedure procedure =
        new Procedure(
            polityId,
            constitution.getId(),
            motion.getInstitutionId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            1,
            2,
            VotingThreshold.PLURALITY_CAST,
            0,
            24,
            EffectType.ELECT_OFFICE);
    ReflectionTestUtils.setField(procedure, "id", motion.getProcedureId());
    OfficeElectionProposal proposal =
        new OfficeElectionProposal(polityId, motion.getId(), officeId);
    OfficeElectionBallot ballot =
        new OfficeElectionBallot(polityId, motion.getId(), actor.getId(), winnerMembershipId, NOW);

    when(officeElectionProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(Optional.of(projection(OfficeElectionProposalProjection.class, proposal)));
    when(offices.findEntityByIdAndPolityId(officeId, polityId)).thenReturn(Optional.of(office));
    when(procedures.findEntityById(motion.getProcedureId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motion.getId())).thenReturn(1L);
    when(officeElectionCandidates.findEntitiesByMotionIdAndStatus(
            motion.getId(), OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(
            List.of(new OfficeElectionCandidate(polityId, motion.getId(), winnerMembershipId)));
    when(officeElectionBallots.findEntitiesByMotionId(motion.getId())).thenReturn(List.of(ballot));
    when(membershipService.get(winnerMembershipId)).thenReturn(winner);
    when(membershipService.hasPoliticalStanding(winner, NOW)).thenReturn(true);
    when(officeTerms.countByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, Office.STEWARD, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(1L);

    assertThatThrownBy(() -> service.apply(motion, actor, constitution, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessage("This office has no vacant seats for another active term.");

    verify(officeTerms, never()).saveAndFlush(any(OfficeTerm.class));
  }

  @Test
  void appealEffectRejectsExpiredSanctionsAtCertificationTime() {
    UUID polityId = UUID.randomUUID();
    UUID sanctionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion = motion(polityId, actor.getId(), constitution.getId(), EffectType.GRANT_APPEAL);
    Sanction sanction =
        new Sanction(
            polityId,
            UUID.randomUUID(),
            actor.getId(),
            SanctionType.WARNING,
            "Reason",
            NOW.minusDays(2),
            NOW.minusDays(1));
    ReflectionTestUtils.setField(sanction, "id", sanctionId);
    AppealProposal proposal =
        new AppealProposal(polityId, motion.getId(), sanctionId, actor.getId(), "Evidence");

    when(appealProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(Optional.of(projection(AppealProposalProjection.class, proposal)));
    when(sanctions.findEntityByIdAndPolityId(sanctionId, polityId))
        .thenReturn(Optional.of(sanction));

    assertThatThrownBy(() -> service.apply(motion, actor, constitution, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessage("Only active sanctions can be appealed.");
  }

  @Test
  void disbandmentEffectMarksPolityDisbandedAndEndsActiveOfficeTerms() {
    UUID polityId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion =
        motion(polityId, actor.getId(), constitution.getId(), EffectType.DISBAND_POLITY);
    Polity polity = new Polity("Friday Council", PolityVisibility.PUBLIC, UUID.randomUUID());
    ReflectionTestUtils.setField(polity, "id", polityId);
    OfficeTerm term =
        new OfficeTerm(
            polityId,
            UUID.randomUUID(),
            Office.STEWARD,
            actor.getId(),
            NOW.minusDays(1),
            NOW.plusDays(1));

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(officeTerms.findEntitiesByPolityIdAndStatus(polityId, OfficeTermStatus.ACTIVE))
        .thenReturn(List.of(term));

    service.apply(motion, actor, constitution, NOW);

    assertThat(polity.getStatus()).isEqualTo(PolityStatus.DISBANDED);
    assertThat(term.getStatus()).isEqualTo(OfficeTermStatus.ENDED);
    verify(polities).saveAndFlush(polity);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.POLITY_DISBANDED),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void officeTermReviewEffectVacatesActiveOfficeTerm() {
    UUID polityId = UUID.randomUUID();
    UUID officeId = UUID.randomUUID();
    UUID termId = UUID.randomUUID();
    UUID holderMembershipId = UUID.randomUUID();
    Membership actor = member(polityId);
    Membership holder = member(polityId);
    ReflectionTestUtils.setField(holder, "id", holderMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion =
        motion(polityId, actor.getId(), constitution.getId(), EffectType.VACATE_OFFICE_TERM);
    Office office =
        new Office(
            polityId,
            constitution.getId(),
            motion.getJurisdictionId(),
            Office.STEWARD,
            "Steward",
            "Coordinates.",
            14);
    ReflectionTestUtils.setField(office, "id", officeId);
    OfficeTerm term =
        new OfficeTerm(
            polityId,
            officeId,
            Office.STEWARD,
            holderMembershipId,
            NOW.minusDays(1),
            NOW.plusDays(7));
    ReflectionTestUtils.setField(term, "id", termId);
    OfficeTermReviewProposal proposal =
        new OfficeTermReviewProposal(polityId, motion.getId(), termId, actor.getId(), "Conflict");

    when(officeTermReviewProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(Optional.of(projection(OfficeTermReviewProposalProjection.class, proposal)));
    when(officeTerms.findEntityByIdAndPolityId(termId, polityId)).thenReturn(Optional.of(term));
    when(offices.findEntityByIdAndPolityId(officeId, polityId)).thenReturn(Optional.of(office));
    when(membershipService.get(holderMembershipId)).thenReturn(holder);
    when(officeTermReviews.saveAndFlush(any(OfficeTermReview.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    service.apply(motion, actor, constitution, NOW);

    assertThat(term.getStatus()).isEqualTo(OfficeTermStatus.ENDED);
    verify(officeTerms).saveAndFlush(term);
    ArgumentCaptor<OfficeTermReview> reviewCaptor = ArgumentCaptor.forClass(OfficeTermReview.class);
    verify(officeTermReviews).saveAndFlush(reviewCaptor.capture());
    assertThat(reviewCaptor.getValue().getOfficeTermId()).isEqualTo(termId);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.OFFICE_TERM_VACATED),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void constitutionalReviewEffectVoidsOfficialActAndVacatesActiveSanction() {
    UUID polityId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    UUID sanctionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion =
        motion(polityId, actor.getId(), constitution.getId(), EffectType.VOID_OFFICIAL_ACT);
    Sanction sanction =
        new Sanction(
            polityId,
            UUID.randomUUID(),
            actor.getId(),
            SanctionType.WARNING,
            "Reason",
            NOW.minusDays(1),
            NOW.plusDays(3));
    ReflectionTestUtils.setField(sanction, "id", sanctionId);
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);
    ConstitutionalReviewProposal proposal =
        new ConstitutionalReviewProposal(
            polityId, motion.getId(), targetRecordId, actor.getId(), "Wrong authority");

    when(targetRecord.getId()).thenReturn(targetRecordId);
    when(targetRecord.getPolityId()).thenReturn(polityId);
    when(targetRecord.getEntryNumber()).thenReturn(9);
    when(targetRecord.getType()).thenReturn(OfficialRecordType.SANCTION_APPLIED);
    when(targetRecord.getSourceId()).thenReturn(sanctionId);
    when(constitutionalReviewProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionalReviewProposalProjection.class, proposal)));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));
    when(sanctions.findEntityByIdAndPolityId(sanctionId, polityId))
        .thenReturn(Optional.of(sanction));
    when(constitutionalReviews.saveAndFlush(any(ConstitutionalReview.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    service.apply(motion, actor, constitution, NOW);

    assertThat(sanction.getStatus()).isEqualTo(com.odonta.polity.model.SanctionStatus.VACATED);
    verify(sanctions).saveAndFlush(sanction);
    ArgumentCaptor<ConstitutionalReview> reviewCaptor =
        ArgumentCaptor.forClass(ConstitutionalReview.class);
    verify(constitutionalReviews).saveAndFlush(reviewCaptor.capture());
    assertThat(reviewCaptor.getValue().getTargetRecordId()).isEqualTo(targetRecordId);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.OFFICIAL_ACT_VOIDED),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void constitutionalReviewEffectVoidsAdoptedResolution() {
    UUID polityId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    UUID resolutionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion =
        motion(polityId, actor.getId(), constitution.getId(), EffectType.VOID_OFFICIAL_ACT);
    Resolution resolution =
        new Resolution(
            polityId, UUID.randomUUID(), "Budget resolution", "Buy more gavels.", NOW.minusDays(1));
    ReflectionTestUtils.setField(resolution, "id", resolutionId);
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);
    ConstitutionalReviewProposal proposal =
        new ConstitutionalReviewProposal(
            polityId, motion.getId(), targetRecordId, actor.getId(), "Unconstitutional");

    when(targetRecord.getId()).thenReturn(targetRecordId);
    when(targetRecord.getPolityId()).thenReturn(polityId);
    when(targetRecord.getEntryNumber()).thenReturn(11);
    when(targetRecord.getType()).thenReturn(OfficialRecordType.RESOLUTION_ADOPTED);
    when(targetRecord.getSourceId()).thenReturn(resolutionId);
    when(constitutionalReviewProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionalReviewProposalProjection.class, proposal)));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));
    when(resolutions.findEntityByIdAndPolityId(resolutionId, polityId))
        .thenReturn(Optional.of(resolution));
    when(constitutionalReviews.saveAndFlush(any(ConstitutionalReview.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    service.apply(motion, actor, constitution, NOW);

    assertThat(resolution.getStatus()).isEqualTo(ResolutionStatus.VOIDED);
    assertThat(resolution.getVoidedAt()).isEqualTo(NOW);
    verify(resolutions).saveAndFlush(resolution);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.OFFICIAL_ACT_VOIDED),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void constitutionalReviewEffectFailsWhenVoidRemedyIsUnavailable() {
    UUID polityId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    UUID resolutionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion =
        motion(polityId, actor.getId(), constitution.getId(), EffectType.VOID_OFFICIAL_ACT);
    Resolution resolution =
        new Resolution(
            polityId, UUID.randomUUID(), "Budget resolution", "Buy more gavels.", NOW.minusDays(1));
    resolution.voidAt(NOW.minusHours(1));
    ReflectionTestUtils.setField(resolution, "id", resolutionId);
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);
    ConstitutionalReviewProposal proposal =
        new ConstitutionalReviewProposal(
            polityId, motion.getId(), targetRecordId, actor.getId(), "Unconstitutional");

    when(targetRecord.getId()).thenReturn(targetRecordId);
    when(targetRecord.getPolityId()).thenReturn(polityId);
    when(targetRecord.getType()).thenReturn(OfficialRecordType.RESOLUTION_ADOPTED);
    when(targetRecord.getSourceId()).thenReturn(resolutionId);
    when(constitutionalReviewProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionalReviewProposalProjection.class, proposal)));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));
    when(resolutions.findEntityByIdAndPolityId(resolutionId, polityId))
        .thenReturn(Optional.of(resolution));

    assertThatThrownBy(() -> service.apply(motion, actor, constitution, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("no longer has an active remedy");

    verify(constitutionalReviews, never()).saveAndFlush(any());
    verify(officialRecords, never())
        .append(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  private Motion motion(UUID polityId, UUID actorMembershipId, UUID constitutionId) {
    return motion(polityId, actorMembershipId, constitutionId, EffectType.AMEND_CONSTITUTION);
  }

  private Motion motion(
      UUID polityId, UUID actorMembershipId, UUID constitutionId, EffectType effectType) {
    Motion motion =
        new Motion(
            polityId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            constitutionId,
            UUID.randomUUID(),
            actorMembershipId,
            "Amend constitution",
            "New body",
            effectType,
            NOW.minusHours(1),
            NOW.minusHours(1),
            NOW.plusHours(1),
            NOW.plusHours(1));
    ReflectionTestUtils.setField(motion, "id", UUID.randomUUID());
    return motion;
  }

  private ConstitutionVersion constitution(UUID polityId, UUID constitutionId, String body) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", body, NOW.minusDays(1));
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    return constitution;
  }

  private Membership member(UUID polityId) {
    Membership member =
        new Membership(
            polityId, UUID.randomUUID(), "subject", "friend@example.com", "Friend", NOW, null);
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private Jurisdiction jurisdiction(UUID polityId, UUID jurisdictionId) {
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", jurisdictionId);
    return jurisdiction;
  }

  private <T> T withId(T entity) {
    if (ReflectionTestUtils.getField(entity, "id") == null) {
      ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
    return entity;
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
