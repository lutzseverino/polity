package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.common.api.ApiException;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import com.odonta.polity.model.ConstitutionPowerChangeProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionBallot;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionProposal;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class EffectApplicationServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-14T10:00:00Z");

  private final AppealProposalRepository appealProposals = mock(AppealProposalRepository.class);
  private final AppealRepository appeals = mock(AppealRepository.class);
  private final ConstitutionAmendmentProposalRepository amendmentProposals =
      mock(ConstitutionAmendmentProposalRepository.class);
  private final ConstitutionOfficeChangeProposalRepository officeChangeProposals =
      mock(ConstitutionOfficeChangeProposalRepository.class);
  private final ConstitutionPowerChangeProposalRepository powerChangeProposals =
      mock(ConstitutionPowerChangeProposalRepository.class);
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals =
      mock(ConstitutionProcedureChangeProposalRepository.class);
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final InstitutionRepository institutions = mock(InstitutionRepository.class);
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
  private final PolityRepository polities = mock(PolityRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final ResolutionRepository resolutions = mock(ResolutionRepository.class);
  private final SanctionProposalRepository sanctionProposals =
      mock(SanctionProposalRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private final MembershipService membershipService = mock(MembershipService.class);

  private final EffectApplicationService service =
      new EffectApplicationService(
          appealProposals,
          appeals,
          amendmentProposals,
          officeChangeProposals,
          powerChangeProposals,
          procedureChangeProposals,
          constitutions,
          powers,
          institutions,
          electors,
          officeElectionBallots,
          officeElectionCandidates,
          officeElections,
          officeElectionProposals,
          offices,
          officeTerms,
          polities,
          officialRecords,
          procedures,
          resolutions,
          sanctionProposals,
          sanctions,
          membershipService);

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
            VotingThreshold.TWO_THIRDS_CAST,
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

    when(amendmentProposals.findByMotionId(motion.getId())).thenReturn(Optional.of(proposal));
    when(procedureChangeProposals.findByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of(procedureChange));
    when(officeChangeProposals.findByAmendmentProposalId(proposal.getId())).thenReturn(List.of());
    when(powerChangeProposals.findByAmendmentProposalId(proposal.getId())).thenReturn(List.of());
    when(constitutions.findById(currentConstitutionId)).thenReturn(Optional.of(current));
    when(constitutions.saveAndFlush(any(ConstitutionVersion.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(institutions.findByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(institution));
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(procedures.findByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(procedure));
    when(offices.findByConstitutionVersionIdOrderByName(currentConstitutionId))
        .thenReturn(List.of());
    when(powers.findByConstitutionVersionId(currentConstitutionId)).thenReturn(List.of());

    service.apply(motion, actor, current, NOW);

    assertThat(current.getStatus()).isEqualTo(ConstitutionStatus.SUPERSEDED);
    ArgumentCaptor<ConstitutionVersion> constitutionCaptor =
        ArgumentCaptor.forClass(ConstitutionVersion.class);
    verify(constitutions, org.mockito.Mockito.times(2)).saveAndFlush(constitutionCaptor.capture());
    ConstitutionVersion amended = constitutionCaptor.getAllValues().get(1);
    assertThat(amended.getVersion()).isEqualTo(2);
    assertThat(amended.getTitle())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackTitle(2));
    assertThat(amended.getBody())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackBody());
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
            "Clerk",
            "Keeps the rolls.",
            30);
    ConstitutionPowerChangeProposal powerChange =
        new ConstitutionPowerChangeProposal(
            polityId, proposal.getId(), PowerCode.ADMIT_MEMBER, PowerHolderScope.OFFICE, "clerk");

    when(amendmentProposals.findByMotionId(motion.getId())).thenReturn(Optional.of(proposal));
    when(procedureChangeProposals.findByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(officeChangeProposals.findByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of(officeChange));
    when(powerChangeProposals.findByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of(powerChange));
    when(constitutions.findById(currentConstitutionId)).thenReturn(Optional.of(current));
    when(constitutions.saveAndFlush(any(ConstitutionVersion.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(institutions.findByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(institution));
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(procedures.findByConstitutionVersionId(currentConstitutionId)).thenReturn(List.of());
    when(offices.findByConstitutionVersionIdOrderByName(currentConstitutionId))
        .thenReturn(List.of(steward));
    when(powers.findByConstitutionVersionId(currentConstitutionId))
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

    when(officeElectionProposals.findByMotionId(motion.getId())).thenReturn(Optional.of(proposal));
    when(offices.findByIdAndPolityId(officeId, polityId)).thenReturn(Optional.of(office));
    when(procedures.findById(motion.getProcedureId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motion.getId())).thenReturn(3L);
    when(officeElectionCandidates.findByMotionIdAndStatus(
            motion.getId(), OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(
            List.of(
                new OfficeElectionCandidate(polityId, motion.getId(), winnerMembershipId),
                new OfficeElectionCandidate(polityId, motion.getId(), otherCandidateMembershipId)));
    when(officeElectionBallots.findByMotionId(motion.getId())).thenReturn(List.of(first, second));
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
  void legacyOfficeAssignmentEffectIsReadableButNoLongerExecutable() {
    UUID polityId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion = motion(polityId, actor.getId(), constitution.getId(), EffectType.ASSIGN_OFFICE);

    assertThatThrownBy(() -> service.apply(motion, actor, constitution, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessage("Direct office assignment is a legacy effect and can no longer be applied.");
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

    when(appealProposals.findByMotionId(motion.getId())).thenReturn(Optional.of(proposal));
    when(sanctions.findByIdAndPolityId(sanctionId, polityId)).thenReturn(Optional.of(sanction));

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

    when(polities.findById(polityId)).thenReturn(Optional.of(polity));
    when(officeTerms.findByPolityIdAndStatus(polityId, OfficeTermStatus.ACTIVE))
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

  private <T> T withId(T entity) {
    if (ReflectionTestUtils.getField(entity, "id") == null) {
      ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
    return entity;
  }
}
