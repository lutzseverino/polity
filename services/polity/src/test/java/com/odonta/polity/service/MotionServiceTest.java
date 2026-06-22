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

import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.evaluator.VotingEvaluator;
import com.odonta.polity.mapper.MotionApplicationMapper;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.CastOfficeElectionBallotInput;
import com.odonta.polity.model.CastVoteInput;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.CertificationModality;
import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.CreateAppealMotionInput;
import com.odonta.polity.model.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.model.CreateDisbandmentMotionInput;
import com.odonta.polity.model.CreateOfficeChangeInput;
import com.odonta.polity.model.CreateOfficeElectionMotionInput;
import com.odonta.polity.model.CreatePowerChangeInput;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionBallot;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.RespondOfficeElectionCandidacyInput;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.repository.VoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

class MotionServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-11T20:00:00Z");

  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final AppealRepository appeals = mock(AppealRepository.class);
  private final AppealProposalRepository appealProposals = mock(AppealProposalRepository.class);
  private final CertificationRepository certifications = mock(CertificationRepository.class);
  private final ConstitutionalAuthority authority = mock(ConstitutionalAuthority.class);
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
  private final EffectApplicationService effects = mock(EffectApplicationService.class);
  private final MotionElectorRepository electors = mock(MotionElectorRepository.class);
  private final MembershipService membershipService = mock(MembershipService.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final MotionRepository motions = mock(MotionRepository.class);
  private final OfficeElectionBallotRepository officeElectionBallots =
      mock(OfficeElectionBallotRepository.class);
  private final OfficeElectionCandidateRepository officeElectionCandidates =
      mock(OfficeElectionCandidateRepository.class);
  private final OfficeElectionEvaluator officeElections = new OfficeElectionEvaluator();
  private final OfficeElectionProposalRepository officeElectionProposals =
      mock(OfficeElectionProposalRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final PolityService polities = mock(PolityService.class);
  private final ProcedureElectorateService procedureElectorates =
      mock(ProcedureElectorateService.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final SanctionProposalRepository sanctionProposals =
      mock(SanctionProposalRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private final VoteRepository votes = mock(VoteRepository.class);
  private MotionService service;

  @BeforeEach
  void setUp() {
    service =
        new MotionService(
            Clock.fixed(Instant.from(NOW), ZoneOffset.UTC),
            access,
            appeals,
            appealProposals,
            certifications,
            authority,
            amendmentProposals,
            officeChangeProposals,
            powerChangeProposals,
            procedureChangeProposals,
            constitutions,
            powers,
            effects,
            electors,
            membershipService,
            memberships,
            Mappers.getMapper(MotionApplicationMapper.class),
            motions,
            officeElectionBallots,
            officeElectionCandidates,
            officeElections,
            officeElectionProposals,
            offices,
            officialRecords,
            polities,
            procedureElectorates,
            procedures,
            sanctionProposals,
            sanctions,
            votes,
            new VotingEvaluator());
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);
    when(procedureElectorates.electors(any(Procedure.class), any(OffsetDateTime.class)))
        .thenReturn(List.of());
  }

  @Test
  void rejectsVotesFromMembersOutsideTheFrozenElectorate() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId, UUID.randomUUID());
    Motion motion = motion(polityId, motionId, member.getId());
    when(membershipService.active(polityId, userId)).thenReturn(member);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(electors.existsByMotionIdAndMembershipId(motionId, member.getId())).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.vote(
                    polityId,
                    motionId,
                    new AuthenticatedUser(userId, "subject", "Late Member"),
                    new CastVoteInput(VoteChoice.YES)))
        .isInstanceOf(ApiException.class)
        .hasMessage("This member was not eligible when voting opened.");
  }

  @Test
  void certificationUsesTheFrozenElectorateAndAdoptsAResolution() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    UUID requesterMembershipId = UUID.randomUUID();
    Membership requester = member(polityId, requesterUserId, requesterMembershipId);
    Motion motion = motion(polityId, motionId, requesterMembershipId);
    ReflectionTestUtils.setField(motion, "votingClosesAt", NOW.minusMinutes(1));
    ReflectionTestUtils.setField(motion, "certificationOpensAt", NOW.minusMinutes(1));
    ConstitutionVersion constitution = constitution(polityId, motion.getConstitutionVersionId());
    Procedure procedure = procedure(polityId, motion.getProcedureId(), constitution.getId());
    Vote first = yesVote(polityId, motionId, requesterMembershipId);
    Vote second = yesVote(polityId, motionId, UUID.randomUUID());

    when(membershipService.active(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findById(constitution.getId())).thenReturn(Optional.of(constitution));
    when(procedures.findById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motionId)).thenReturn(3L);
    when(votes.findByMotionId(motionId)).thenReturn(List.of(first, second));
    when(certifications.saveAndFlush(any(Certification.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(memberships.findById(requesterMembershipId)).thenReturn(Optional.of(requester));
    when(certifications.findByMotionId(motionId))
        .thenAnswer(
            invocation -> {
              Certification certification =
                  new Certification(
                      polityId,
                      motionId,
                      requesterMembershipId,
                      new VotingEvaluator().evaluate(procedure, 3, List.of(first, second)),
                      NOW);
              return Optional.of(certification);
            });
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));

    var result =
        service.certify(
            polityId, motionId, new AuthenticatedUser(requesterUserId, "subject", "Requester"));

    assertThat(result.status()).isEqualTo(MotionStatus.ENACTED);
    assertThat(result.tally().eligible()).isEqualTo(3);
    assertThat(result.tally().passed()).isTrue();
    verify(authority).require(requester, constitution, PowerCode.REQUEST_CERTIFICATION);
    verify(effects).apply(motion, requester, constitution, NOW);
    verify(officialRecords).append(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void rejectsCertificationBeforeTheFrozenCertificationWindow() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    UUID requesterMembershipId = UUID.randomUUID();
    Membership requester = member(polityId, requesterUserId, requesterMembershipId);
    Motion motion = motion(polityId, motionId, requesterMembershipId);
    ConstitutionVersion constitution = constitution(polityId, motion.getConstitutionVersionId());

    when(membershipService.active(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findById(constitution.getId())).thenReturn(Optional.of(constitution));

    assertThatThrownBy(
            () ->
                service.certify(
                    polityId,
                    motionId,
                    new AuthenticatedUser(requesterUserId, "subject", "Requester")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This motion cannot be certified until voting closes.");
  }

  @Test
  void rejectsGoverningMotionsUntilPolityReachesFullGovernmentSize() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    doThrow(
            ApiException.conflict(
                "polity_provisional",
                "This polity needs at least three citizens with political standing before full government motions can be introduced."))
        .when(polities)
        .requireFullGovernment(polityId);

    assertThatThrownBy(
            () ->
                service.create(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new com.odonta.polity.model.CreateMotionInput(
                        "Paint the plaza", "Ceremonially, but enforceably.")))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "This polity needs at least three citizens with political standing before full government motions can be introduced.");

    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_MOTION);
  }

  @Test
  void createOfficeElectionPersistsCandidateSlate() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    UUID candidateMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    Membership candidate = member(polityId, UUID.randomUUID(), candidateMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Office office = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            EffectType.ELECT_OFFICE,
            VotingThreshold.PLURALITY_CAST);
    Motion[] saved = new Motion[1];

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.OFFICE_ELECTION))
        .thenReturn(Optional.of(procedure));
    when(offices.findByIdAndPolityId(office.getId(), polityId)).thenReturn(Optional.of(office));
    when(memberships.findById(candidateMembershipId)).thenReturn(Optional.of(candidate));
    when(membershipService.get(candidateMembershipId)).thenReturn(candidate);
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(officeElectionProposals.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(officeElectionProposals.findByMotionId(any(UUID.class)))
        .thenAnswer(
            invocation ->
                Optional.of(
                    new com.odonta.polity.model.OfficeElectionProposal(
                        polityId, invocation.getArgument(0), office.getId())));
    when(memberships.findByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, com.odonta.polity.model.MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor, candidate));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(certifications.findByMotionId(any(UUID.class))).thenReturn(Optional.empty());
    when(electors.countByMotionId(any(UUID.class))).thenReturn(2L);
    when(votes.findByMotionId(any(UUID.class))).thenReturn(List.of());
    when(officeElectionCandidates.findByMotionIdAndStatus(
            any(UUID.class), eq(OfficeElectionCandidateStatus.ACCEPTED)))
        .thenReturn(List.of());
    when(officeElectionCandidates.findByMotionId(any(UUID.class)))
        .thenAnswer(
            invocation ->
                List.of(
                    new OfficeElectionCandidate(
                        polityId,
                        invocation.getArgument(0),
                        candidateMembershipId,
                        OfficeElectionCandidateStatus.PENDING,
                        null)));
    when(officeElectionBallots.findByMotionId(any(UUID.class))).thenReturn(List.of());

    var result =
        service.createOfficeElection(
            polityId,
            new AuthenticatedUser(actorUserId, "subject", "Requester"),
            new CreateOfficeElectionMotionInput(office.getId(), List.of(candidateMembershipId)));

    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_OFFICE_ELECTION);
    verify(officeElectionProposals).saveAndFlush(any());
    verify(officeElectionCandidates)
        .saveAllAndFlush(
            ArgumentMatchers.argThat(
                candidates -> {
                  OfficeElectionCandidate only = candidates.iterator().next();
                  return count(candidates) == 1
                      && only.getStatus() == OfficeElectionCandidateStatus.PENDING;
                }));
    assertThat(result.officeElection().officeId()).isEqualTo(office.getId());
    assertThat(result.officeElection().officeCode()).isEqualTo(Office.STEWARD);
    assertThat(result.officeElection().candidates()).hasSize(1);
    assertThat(result.officeElection().candidates().getFirst().status())
        .isEqualTo(OfficeElectionCandidateStatus.PENDING);
    assertThat(saved[0].getTitleKey()).isEqualTo("motion.office_election.title");
    assertThat(saved[0].getBodyKey()).isEqualTo("motion.office_election.body");
    assertThat(saved[0].getTemplateParams()).containsEntry("officeName", office.getName());
  }

  @Test
  void certificationUsesElectionBallotsAndElectsWinner() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    UUID requesterMembershipId = UUID.randomUUID();
    UUID winnerMembershipId = UUID.randomUUID();
    UUID otherCandidateMembershipId = UUID.randomUUID();
    Membership requester = member(polityId, requesterUserId, requesterMembershipId);
    Membership winner = member(polityId, UUID.randomUUID(), winnerMembershipId);
    Membership otherCandidate = member(polityId, UUID.randomUUID(), otherCandidateMembershipId);
    Motion motion = motion(polityId, motionId, requesterMembershipId, EffectType.ELECT_OFFICE);
    ReflectionTestUtils.setField(motion, "votingClosesAt", NOW.minusMinutes(1));
    ReflectionTestUtils.setField(motion, "certificationOpensAt", NOW.minusMinutes(1));
    ConstitutionVersion constitution = constitution(polityId, motion.getConstitutionVersionId());
    Procedure procedure =
        procedure(
            polityId,
            motion.getProcedureId(),
            constitution.getId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            EffectType.ELECT_OFFICE,
            VotingThreshold.PLURALITY_CAST);
    OfficeElectionBallot first =
        new OfficeElectionBallot(
            polityId, motionId, requesterMembershipId, winnerMembershipId, NOW);
    OfficeElectionBallot second =
        new OfficeElectionBallot(polityId, motionId, UUID.randomUUID(), winnerMembershipId, NOW);

    when(membershipService.active(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findById(constitution.getId())).thenReturn(Optional.of(constitution));
    when(procedures.findById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motionId)).thenReturn(3L);
    when(officeElectionCandidates.findByMotionIdAndStatus(
            motionId, OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(
            List.of(
                new OfficeElectionCandidate(polityId, motionId, winnerMembershipId),
                new OfficeElectionCandidate(polityId, motionId, otherCandidateMembershipId)));
    when(memberships.findById(winnerMembershipId)).thenReturn(Optional.of(winner));
    when(memberships.findById(otherCandidateMembershipId)).thenReturn(Optional.of(otherCandidate));
    when(officeElectionBallots.findByMotionId(motionId)).thenReturn(List.of(first, second));
    when(certifications.saveAndFlush(any(Certification.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(certifications.findByMotionId(motionId)).thenReturn(Optional.empty());
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));
    when(votes.findByMotionId(motionId)).thenReturn(List.of());

    var result =
        service.certify(
            polityId, motionId, new AuthenticatedUser(requesterUserId, "subject", "Requester"));

    assertThat(result.status()).isEqualTo(MotionStatus.ENACTED);
    assertThat(result.tally()).isNull();
    assertThat(result.electionTally().winnerMembershipId()).isEqualTo(winnerMembershipId);
    ArgumentCaptor<Certification> certificationCaptor =
        ArgumentCaptor.forClass(Certification.class);
    verify(certifications).saveAndFlush(certificationCaptor.capture());
    assertThat(certificationCaptor.getValue().getModality())
        .isEqualTo(CertificationModality.OFFICE_ELECTION);
    assertThat(certificationCaptor.getValue().getYesCount()).isNull();
    assertThat(certificationCaptor.getValue().getNoCount()).isNull();
    assertThat(certificationCaptor.getValue().getAbstainCount()).isNull();
    assertThat(certificationCaptor.getValue().getElectionParticipationCount()).isEqualTo(2);
    assertThat(certificationCaptor.getValue().getElectionDecisive()).isTrue();
    assertThat(certificationCaptor.getValue().getElectionWinnerMembershipId())
        .isEqualTo(winnerMembershipId);
    assertThat(certificationCaptor.getValue().getElectionWinnerName())
        .isEqualTo(winner.getDisplayName());
    verify(effects).apply(motion, requester, constitution, NOW);
  }

  @Test
  void certificationDisqualifiesOfficeElectionCandidatesWithoutStanding() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    UUID requesterMembershipId = UUID.randomUUID();
    UUID disqualifiedMembershipId = UUID.randomUUID();
    UUID otherCandidateMembershipId = UUID.randomUUID();
    Membership requester = member(polityId, requesterUserId, requesterMembershipId);
    Membership disqualified = member(polityId, UUID.randomUUID(), disqualifiedMembershipId);
    Membership otherCandidate = member(polityId, UUID.randomUUID(), otherCandidateMembershipId);
    Motion motion = motion(polityId, motionId, requesterMembershipId, EffectType.ELECT_OFFICE);
    ReflectionTestUtils.setField(motion, "votingClosesAt", NOW.minusMinutes(1));
    ReflectionTestUtils.setField(motion, "certificationOpensAt", NOW.minusMinutes(1));
    ConstitutionVersion constitution = constitution(polityId, motion.getConstitutionVersionId());
    Procedure procedure =
        procedure(
            polityId,
            motion.getProcedureId(),
            constitution.getId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            EffectType.ELECT_OFFICE,
            VotingThreshold.PLURALITY_CAST);
    OfficeElectionCandidate disqualifiedCandidate =
        new OfficeElectionCandidate(polityId, motionId, disqualifiedMembershipId);
    OfficeElectionCandidate otherCandidateOption =
        new OfficeElectionCandidate(polityId, motionId, otherCandidateMembershipId);
    OfficeElectionBallot ballot =
        new OfficeElectionBallot(
            polityId, motionId, requesterMembershipId, disqualifiedMembershipId, NOW);

    when(membershipService.active(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findById(constitution.getId())).thenReturn(Optional.of(constitution));
    when(procedures.findById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motionId)).thenReturn(3L);
    when(officeElectionCandidates.findByMotionIdAndStatus(
            motionId, OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(List.of(disqualifiedCandidate, otherCandidateOption))
        .thenReturn(List.of(otherCandidateOption))
        .thenReturn(List.of(otherCandidateOption));
    when(memberships.findById(disqualifiedMembershipId)).thenReturn(Optional.of(disqualified));
    when(memberships.findById(otherCandidateMembershipId)).thenReturn(Optional.of(otherCandidate));
    when(membershipService.hasPoliticalStanding(disqualified, NOW)).thenReturn(false);
    when(membershipService.hasPoliticalStanding(otherCandidate, NOW)).thenReturn(true);
    when(officeElectionBallots.findByMotionId(motionId)).thenReturn(List.of(ballot));
    when(certifications.saveAndFlush(any(Certification.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(certifications.findByMotionId(motionId)).thenReturn(Optional.empty());
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));
    when(votes.findByMotionId(motionId)).thenReturn(List.of());

    var result =
        service.certify(
            polityId, motionId, new AuthenticatedUser(requesterUserId, "subject", "Requester"));

    assertThat(disqualifiedCandidate.getStatus())
        .isEqualTo(OfficeElectionCandidateStatus.DISQUALIFIED);
    assertThat(result.status()).isEqualTo(MotionStatus.REJECTED);
    assertThat(result.electionTally().winnerMembershipId()).isNull();
    verify(officeElectionCandidates).saveAllAndFlush(List.of(disqualifiedCandidate));
    verify(effects, never()).apply(motion, requester, constitution, NOW);
  }

  @Test
  void candidateCanAcceptOfficeElectionNomination() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID candidateUserId = UUID.randomUUID();
    UUID candidateMembershipId = UUID.randomUUID();
    Membership candidate = member(polityId, candidateUserId, candidateMembershipId);
    Motion motion = motion(polityId, motionId, UUID.randomUUID(), EffectType.ELECT_OFFICE);
    ReflectionTestUtils.setField(motion, "votingOpensAt", NOW.plusMinutes(30));
    OfficeElectionCandidate candidacy =
        new OfficeElectionCandidate(
            polityId, motionId, candidateMembershipId, OfficeElectionCandidateStatus.PENDING, null);
    Procedure procedure =
        procedure(
            polityId,
            motion.getProcedureId(),
            motion.getConstitutionVersionId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            EffectType.ELECT_OFFICE,
            VotingThreshold.PLURALITY_CAST);

    when(membershipService.active(polityId, candidateUserId)).thenReturn(candidate);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(officeElectionCandidates.findByMotionIdAndMembershipId(motionId, candidateMembershipId))
        .thenReturn(Optional.of(candidacy));
    when(officeElectionCandidates.saveAndFlush(any(OfficeElectionCandidate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));
    when(certifications.findByMotionId(motionId)).thenReturn(Optional.empty());
    when(electors.countByMotionId(motionId)).thenReturn(1L);
    when(votes.findByMotionId(motionId)).thenReturn(List.of());
    when(officeElectionCandidates.findByMotionIdAndStatus(
            motionId, OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(List.of(candidacy));
    when(officeElectionBallots.findByMotionId(motionId)).thenReturn(List.of());
    when(memberships.findById(candidateMembershipId)).thenReturn(Optional.of(candidate));

    service.respondOfficeElectionCandidacy(
        polityId,
        motionId,
        new AuthenticatedUser(candidateUserId, "subject", "Candidate"),
        new RespondOfficeElectionCandidacyInput(true));

    assertThat(candidacy.getStatus()).isEqualTo(OfficeElectionCandidateStatus.ACCEPTED);
    assertThat(candidacy.getRespondedAt()).isEqualTo(NOW);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            eq(OfficialRecordType.CANDIDACY_RESPONDED),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void rejectsCandidacyResponseAfterVotingOpens() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID candidateUserId = UUID.randomUUID();
    UUID candidateMembershipId = UUID.randomUUID();
    Membership candidate = member(polityId, candidateUserId, candidateMembershipId);
    Motion motion = motion(polityId, motionId, UUID.randomUUID(), EffectType.ELECT_OFFICE);
    ReflectionTestUtils.setField(motion, "votingOpensAt", NOW.minusMinutes(1));

    when(membershipService.active(polityId, candidateUserId)).thenReturn(candidate);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));

    assertThatThrownBy(
            () ->
                service.respondOfficeElectionCandidacy(
                    polityId,
                    motionId,
                    new AuthenticatedUser(candidateUserId, "subject", "Candidate"),
                    new RespondOfficeElectionCandidacyInput(true)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Candidate responses close when voting opens.");

    verify(officeElectionCandidates, never()).saveAndFlush(any());
  }

  @Test
  void rejectsElectionBallotsForPendingCandidates() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID voterUserId = UUID.randomUUID();
    UUID voterMembershipId = UUID.randomUUID();
    UUID candidateMembershipId = UUID.randomUUID();
    Membership voter = member(polityId, voterUserId, voterMembershipId);
    Motion motion = motion(polityId, motionId, voterMembershipId, EffectType.ELECT_OFFICE);

    when(membershipService.active(polityId, voterUserId)).thenReturn(voter);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(electors.existsByMotionIdAndMembershipId(motionId, voterMembershipId)).thenReturn(true);
    when(officeElectionCandidates.existsByMotionIdAndMembershipIdAndStatus(
            motionId, candidateMembershipId, OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(false);

    assertThatThrownBy(
            () ->
                service.castOfficeElectionBallot(
                    polityId,
                    motionId,
                    new AuthenticatedUser(voterUserId, "subject", "Voter"),
                    new CastOfficeElectionBallotInput(candidateMembershipId)))
        .isInstanceOf(ApiException.class)
        .hasMessage("This member is not an accepted candidate in the election.");
  }

  @Test
  void certificationOfOwnAppealUsesAppealCertificationAuthority() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    UUID requesterMembershipId = UUID.randomUUID();
    Membership requester = member(polityId, requesterUserId, requesterMembershipId);
    Motion motion = motion(polityId, motionId, requesterMembershipId, EffectType.GRANT_APPEAL);
    ReflectionTestUtils.setField(motion, "votingClosesAt", NOW.minusMinutes(1));
    ReflectionTestUtils.setField(motion, "certificationOpensAt", NOW.minusMinutes(1));
    ConstitutionVersion constitution = constitution(polityId, motion.getConstitutionVersionId());
    Procedure procedure =
        procedure(
            polityId,
            motion.getProcedureId(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    AppealProposal proposal =
        new AppealProposal(
            polityId, motionId, UUID.randomUUID(), requesterMembershipId, "Standing appeal");
    Vote vote = yesVote(polityId, motionId, requesterMembershipId);

    when(membershipService.active(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findById(constitution.getId())).thenReturn(Optional.of(constitution));
    when(appealProposals.findByMotionId(motionId)).thenReturn(Optional.of(proposal));
    when(procedures.findById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motionId)).thenReturn(1L);
    when(votes.findByMotionId(motionId)).thenReturn(List.of(vote));
    when(certifications.saveAndFlush(any(Certification.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(certifications.findByMotionId(motionId)).thenReturn(Optional.empty());
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));

    service.certify(
        polityId, motionId, new AuthenticatedUser(requesterUserId, "subject", "Requester"));

    verify(authority).requireAppealCertification(requester, constitution);
    verify(authority, never()).require(requester, constitution, PowerCode.REQUEST_CERTIFICATION);
  }

  @Test
  void rejectsYesNoVotesOnOfficeElectionMotions() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID voterUserId = UUID.randomUUID();
    UUID voterMembershipId = UUID.randomUUID();
    Membership voter = member(polityId, voterUserId, voterMembershipId);
    Motion motion = motion(polityId, motionId, voterMembershipId, EffectType.ELECT_OFFICE);

    when(membershipService.active(polityId, voterUserId)).thenReturn(voter);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));

    assertThatThrownBy(
            () ->
                service.vote(
                    polityId,
                    motionId,
                    new AuthenticatedUser(voterUserId, "subject", "Voter"),
                    new CastVoteInput(VoteChoice.YES)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Office elections require an election ballot, not a yes/no vote.");
  }

  @Test
  void rejectsOfficeElectionCandidatesWithoutPoliticalStanding() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    UUID candidateMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    Membership candidate = member(polityId, UUID.randomUUID(), candidateMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Office office = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            EffectType.ELECT_OFFICE,
            VotingThreshold.PLURALITY_CAST);

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.OFFICE_ELECTION))
        .thenReturn(Optional.of(procedure));
    when(offices.findByIdAndPolityId(office.getId(), polityId)).thenReturn(Optional.of(office));
    when(memberships.findById(candidateMembershipId)).thenReturn(Optional.of(candidate));
    doThrow(
            ApiException.forbidden(
                "political_standing_required",
                "This member lacks political standing for this constitutional action."))
        .when(membershipService)
        .requirePoliticalStanding(candidate, NOW);

    assertThatThrownBy(
            () ->
                service.createOfficeElection(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateOfficeElectionMotionInput(
                        office.getId(), List.of(candidateMembershipId))))
        .isInstanceOf(ApiException.class)
        .hasMessage("This member lacks political standing for this constitutional action.");
  }

  @Test
  void createDisbandmentUsesConstitutionalProcedureAndPower() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.DISBANDMENT,
            "Disbandment",
            EffectType.DISBAND_POLITY,
            VotingThreshold.TWO_THIRDS_ELIGIBLE);
    Motion[] saved = new Motion[1];

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(constitution.getId(), Procedure.DISBANDMENT))
        .thenReturn(Optional.of(procedure));
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(memberships.findByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, com.odonta.polity.model.MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(certifications.findByMotionId(any(UUID.class))).thenReturn(Optional.empty());
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findByMotionId(any(UUID.class))).thenReturn(List.of());

    service.createDisbandment(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateDisbandmentMotionInput("End the council", "Archive the polity."));

    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_DISBANDMENT);
    verify(polities).requireDisbandmentGovernment(polityId);
    ArgumentCaptor<Motion> motionCaptor = ArgumentCaptor.forClass(Motion.class);
    verify(motions).saveAndFlush(motionCaptor.capture());
    assertThat(motionCaptor.getValue().getEffectType()).isEqualTo(EffectType.DISBAND_POLITY);
  }

  @Test
  void createAmendmentPersistsOfficeAndPowerChanges() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Office steward = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    ConstitutionalPower admitMember =
        new ConstitutionalPower(
            polityId,
            constitution.getId(),
            PowerCode.ADMIT_MEMBER,
            "Admit citizens",
            Office.STEWARD);
    Motion[] saved = new Motion[1];

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(procedure));
    when(offices.findByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(steward));
    when(powers.findByConstitutionVersionId(constitution.getId())).thenReturn(List.of(admitMember));
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(memberships.findByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, com.odonta.polity.model.MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor));
    when(amendmentProposals.saveAndFlush(any()))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(certifications.findByMotionId(any(UUID.class))).thenReturn(Optional.empty());
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findByMotionId(any(UUID.class))).thenReturn(List.of());

    service.createAmendment(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateConstitutionAmendmentMotionInput(
            "Clerkship",
            "Create a clerk and give them admissions.",
            null,
            List.of(
                new CreateOfficeChangeInput(
                    ConstitutionOfficeChangeAction.CREATE,
                    "clerk",
                    "Clerk",
                    "Keeps the citizen roll.",
                    30)),
            List.of(
                new CreatePowerChangeInput(
                    PowerCode.ADMIT_MEMBER, PowerHolderScope.OFFICE, "clerk"))));

    verify(procedureChangeProposals).saveAllAndFlush(List.of());
    verify(officeChangeProposals)
        .saveAllAndFlush(ArgumentMatchers.argThat(changes -> count(changes) == 1));
    verify(powerChangeProposals)
        .saveAllAndFlush(ArgumentMatchers.argThat(changes -> count(changes) == 1));
  }

  @Test
  void rejectsPluralityThresholdOnNonElectionProcedureAmendments() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure amendmentProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Procedure ordinaryProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.ORDINARY_RESOLUTION,
            "Ordinary resolution",
            EffectType.ADOPT_RESOLUTION);

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(amendmentProcedure));
    when(procedures.findByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.ORDINARY_RESOLUTION))
        .thenReturn(Optional.of(ordinaryProcedure));

    assertThatThrownBy(
            () ->
                service.createAmendment(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionAmendmentMotionInput(
                        "Plurality everywhere",
                        "Try to make ordinary motions plurality-based.",
                        List.of(
                            new com.odonta.polity.model.CreateProcedureChangeInput(
                                Procedure.ORDINARY_RESOLUTION,
                                null,
                                null,
                                VotingThreshold.PLURALITY_CAST,
                                null,
                                null,
                                null,
                                null)),
                        null,
                        null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Plurality thresholds can only be used by office election procedures.");
  }

  @Test
  void rejectsAmendmentsThatMoveOfficeElectionInitiativeOutOfCitizenHands() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Office steward = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    ConstitutionalPower officeElection =
        new ConstitutionalPower(
            polityId,
            constitution.getId(),
            PowerCode.INTRODUCE_OFFICE_ELECTION,
            "Propose office elections",
            PowerHolderScope.ACTIVE_MEMBER);

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(procedure));
    when(offices.findByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(steward));
    when(powers.findByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(officeElection));

    assertThatThrownBy(
            () ->
                service.createAmendment(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionAmendmentMotionInput(
                        "Gate elections",
                        "Try to move election initiative to the Steward.",
                        null,
                        null,
                        List.of(
                            new CreatePowerChangeInput(
                                PowerCode.INTRODUCE_OFFICE_ELECTION,
                                PowerHolderScope.OFFICE,
                                Office.STEWARD)))))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "Member initiative, election, appeal, amendment, disbandment, and certification powers must remain active citizen powers.");

    verify(motions, never()).saveAndFlush(any());
  }

  @Test
  void rejectsCertificationForMotionsFromSupersededConstitutions() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    UUID requesterMembershipId = UUID.randomUUID();
    Membership requester = member(polityId, requesterUserId, requesterMembershipId);
    Motion motion = motion(polityId, motionId, requesterMembershipId);
    ReflectionTestUtils.setField(motion, "votingClosesAt", NOW.minusMinutes(1));
    ReflectionTestUtils.setField(motion, "certificationOpensAt", NOW.minusMinutes(1));
    ConstitutionVersion constitution = constitution(polityId, motion.getConstitutionVersionId());
    constitution.supersede();

    when(membershipService.active(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findById(constitution.getId())).thenReturn(Optional.of(constitution));

    assertThatThrownBy(
            () ->
                service.certify(
                    polityId,
                    motionId,
                    new AuthenticatedUser(requesterUserId, "subject", "Requester")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This motion was introduced under a constitution that is no longer ratified.");
  }

  @Test
  void rejectsRetiringOfficeStillHoldingAPower() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Office steward = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    ConstitutionalPower admitMember =
        new ConstitutionalPower(
            polityId,
            constitution.getId(),
            PowerCode.ADMIT_MEMBER,
            "Admit citizens",
            Office.STEWARD);

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(procedure));
    when(offices.findByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(steward));
    when(powers.findByConstitutionVersionId(constitution.getId())).thenReturn(List.of(admitMember));

    assertThatThrownBy(
            () ->
                service.createAmendment(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionAmendmentMotionInput(
                        "Retire steward",
                        "Retire the steward without moving powers.",
                        null,
                        List.of(
                            new CreateOfficeChangeInput(
                                ConstitutionOfficeChangeAction.RETIRE,
                                Office.STEWARD,
                                null,
                                null,
                                null)),
                        null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Office-held powers must refer to an office in the amended constitution.");
  }

  @Test
  void rejectsRetiringOfficeStillDecidingAProcedure() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure amendmentProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Procedure appealProcedure =
        new Procedure(
            polityId,
            constitution.getId(),
            institution.getId(),
            Procedure.APPEAL,
            "Appeal",
            null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            0,
            24,
            EffectType.GRANT_APPEAL);
    Office magistrate =
        new Office(
            polityId,
            constitution.getId(),
            jurisdiction.getId(),
            Office.MAGISTRATE,
            "Magistrate",
            "Decides appeals.",
            14);

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(amendmentProcedure));
    when(procedures.findByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(amendmentProcedure, appealProcedure));
    when(offices.findByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(magistrate));
    when(powers.findByConstitutionVersionId(constitution.getId())).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                service.createAmendment(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionAmendmentMotionInput(
                        "Retire court",
                        "Retire the court without moving appeals.",
                        null,
                        List.of(
                            new CreateOfficeChangeInput(
                                ConstitutionOfficeChangeAction.RETIRE,
                                Office.MAGISTRATE,
                                null,
                                null,
                                null)),
                        null)))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "Office-held procedure electorates must refer to an office in the amended constitution.");
  }

  @Test
  void createAppealUsesTheAppealProcedureEffectType() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    Sanction sanction = sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.plusDays(7));
    Motion[] saved = new Motion[1];

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(procedure));
    when(sanctions.findByIdAndPolityId(sanction.getId(), polityId))
        .thenReturn(Optional.of(sanction));
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(memberships.findByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, com.odonta.polity.model.MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(certifications.findByMotionId(any(UUID.class))).thenReturn(Optional.empty());
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findByMotionId(any(UUID.class))).thenReturn(List.of());

    service.createAppeal(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateAppealMotionInput(sanction.getId(), "Fresh evidence"));

    ArgumentCaptor<Motion> motionCaptor = ArgumentCaptor.forClass(Motion.class);
    verify(motions).saveAndFlush(motionCaptor.capture());
    assertThat(motionCaptor.getValue().getEffectType()).isEqualTo(EffectType.GRANT_APPEAL);
    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_APPEAL);
    verify(polities).requireFullGovernment(polityId);
  }

  @Test
  void createOwnAppealBypassesStandingOnlyForTheSanctionedMember() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    Sanction sanction = sanction(polityId, UUID.randomUUID(), actorMembershipId, NOW.plusDays(7));
    Motion[] saved = new Motion[1];

    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(procedure));
    when(sanctions.findByIdAndPolityId(sanction.getId(), polityId))
        .thenReturn(Optional.of(sanction));
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(memberships.findByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, com.odonta.polity.model.MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(certifications.findByMotionId(any(UUID.class))).thenReturn(Optional.empty());
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findByMotionId(any(UUID.class))).thenReturn(List.of());

    service.createAppeal(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateAppealMotionInput(sanction.getId(), "Fresh evidence"));

    verify(authority).requireOwnAppealIntroduction(actor, constitution);
    verify(authority, never()).require(actor, constitution, PowerCode.INTRODUCE_APPEAL);
    verify(polities, never()).requireFullGovernment(polityId);
  }

  @Test
  void rejectsAppealsForExpiredSanctions() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    Sanction sanction =
        sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.minusMinutes(1));
    stubAppealCreationContext(polityId, actorUserId, sanction);

    assertThatThrownBy(
            () ->
                service.createAppeal(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateAppealMotionInput(sanction.getId(), "Too late")))
        .isInstanceOf(ApiException.class)
        .hasMessage("Only active sanctions can be appealed.");
  }

  @Test
  void rejectsDuplicateGrantedAppeals() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    Sanction sanction = sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.plusDays(7));
    stubAppealCreationContext(polityId, actorUserId, sanction);
    when(appeals.existsByPolityIdAndSanctionId(polityId, sanction.getId())).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.createAppeal(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateAppealMotionInput(sanction.getId(), "Already handled")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This sanction has already been appealed.");
  }

  @Test
  void rejectsDuplicateOpenAppeals() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    Sanction sanction = sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.plusDays(7));
    stubAppealCreationContext(polityId, actorUserId, sanction);
    when(appealProposals.existsByPolityIdAndSanctionIdAndMotionStatus(
            polityId, sanction.getId(), MotionStatus.VOTING))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                service.createAppeal(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateAppealMotionInput(sanction.getId(), "Already open")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This sanction already has an open appeal motion.");
  }

  private Membership member(UUID polityId, UUID userId, UUID membershipId) {
    Membership member =
        new Membership(
            polityId, userId, "subject-" + userId, "friend@example.com", "Friend", NOW, null);
    ReflectionTestUtils.setField(member, "id", membershipId);
    return member;
  }

  private Motion motion(UUID polityId, UUID motionId, UUID introducedBy) {
    return motion(polityId, motionId, introducedBy, EffectType.ADOPT_RESOLUTION);
  }

  private Motion motion(UUID polityId, UUID motionId, UUID introducedBy, EffectType effectType) {
    Motion motion =
        new Motion(
            polityId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            introducedBy,
            "Make Friday officially pizza night",
            "The polity recognizes Friday as pizza night.",
            effectType,
            NOW.minusHours(1),
            NOW.minusHours(1),
            NOW.plusHours(1),
            NOW.plusHours(1));
    ReflectionTestUtils.setField(motion, "id", motionId);
    return motion;
  }

  private ConstitutionVersion constitution(UUID polityId, UUID constitutionId) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", "Body", NOW.minusDays(1));
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    return constitution;
  }

  private Procedure procedure(UUID polityId, UUID procedureId, UUID constitutionId) {
    return procedure(
        polityId,
        procedureId,
        constitutionId,
        Procedure.ORDINARY_RESOLUTION,
        "Ordinary resolution",
        EffectType.ADOPT_RESOLUTION);
  }

  private Procedure procedure(
      UUID polityId,
      UUID procedureId,
      UUID constitutionId,
      String code,
      String name,
      EffectType effectType) {
    return procedure(
        polityId,
        procedureId,
        constitutionId,
        code,
        name,
        effectType,
        VotingThreshold.SIMPLE_MAJORITY_CAST);
  }

  private Procedure procedure(
      UUID polityId,
      UUID procedureId,
      UUID constitutionId,
      String code,
      String name,
      EffectType effectType,
      VotingThreshold threshold) {
    Procedure procedure =
        new Procedure(
            polityId,
            constitutionId,
            UUID.randomUUID(),
            code,
            name,
            1,
            2,
            threshold,
            0,
            24,
            effectType);
    ReflectionTestUtils.setField(procedure, "id", procedureId);
    return procedure;
  }

  private Jurisdiction jurisdiction(UUID polityId) {
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    return jurisdiction;
  }

  private Institution institution(UUID polityId, UUID jurisdictionId, UUID constitutionId) {
    Institution institution =
        new Institution(
            polityId, jurisdictionId, constitutionId, "Assembly", InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(institution, "id", UUID.randomUUID());
    return institution;
  }

  private Office stewardOffice(UUID polityId, UUID constitutionId, UUID jurisdictionId) {
    Office office =
        new Office(
            polityId,
            constitutionId,
            jurisdictionId,
            Office.STEWARD,
            "Steward",
            "Coordinates.",
            14);
    ReflectionTestUtils.setField(office, "id", UUID.randomUUID());
    return office;
  }

  private Sanction sanction(
      UUID polityId, UUID motionId, UUID targetMembershipId, OffsetDateTime endsAt) {
    Sanction sanction =
        new Sanction(
            polityId,
            motionId,
            targetMembershipId,
            SanctionType.WARNING,
            "Reason",
            NOW.minusDays(1),
            endsAt);
    ReflectionTestUtils.setField(sanction, "id", UUID.randomUUID());
    return sanction;
  }

  private void stubAppealCreationContext(UUID polityId, UUID actorUserId, Sanction sanction) {
    Membership actor = member(polityId, actorUserId, UUID.randomUUID());
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    when(membershipService.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(procedure));
    when(sanctions.findByIdAndPolityId(sanction.getId(), polityId))
        .thenReturn(Optional.of(sanction));
  }

  private Vote yesVote(UUID polityId, UUID motionId, UUID membershipId) {
    return new Vote(polityId, motionId, membershipId, VoteChoice.YES, NOW.minusMinutes(10));
  }

  private MotionProjection projection(Motion motion, Procedure procedure) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            motion.getPolityId(), 1, "Starter Constitution", "Body", NOW.minusDays(3));
    ReflectionTestUtils.setField(constitution, "id", motion.getConstitutionVersionId());
    when(constitutions.findById(motion.getConstitutionVersionId()))
        .thenReturn(Optional.of(constitution));
    when(procedures.findById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(membershipService.displayName(motion.getIntroducedBy())).thenReturn("Friend");
    return new MotionProjection() {
      @Override
      public UUID getId() {
        return motion.getId();
      }

      @Override
      public UUID getConstitutionVersionId() {
        return motion.getConstitutionVersionId();
      }

      @Override
      public UUID getProcedureId() {
        return motion.getProcedureId();
      }

      @Override
      public UUID getIntroducedBy() {
        return motion.getIntroducedBy();
      }

      @Override
      public String getTitle() {
        return motion.getTitle();
      }

      @Override
      public String getBody() {
        return motion.getBody();
      }

      @Override
      public String getTitleKey() {
        return motion.getTitleKey();
      }

      @Override
      public String getBodyKey() {
        return motion.getBodyKey();
      }

      @Override
      public Map<String, Object> getTemplateParams() {
        return motion.getTemplateParams();
      }

      @Override
      public MotionStatus getStatus() {
        return motion.getStatus();
      }

      @Override
      public EffectType getEffectType() {
        return motion.getEffectType();
      }

      @Override
      public OffsetDateTime getOpenedAt() {
        return motion.getOpenedAt();
      }

      @Override
      public OffsetDateTime getVotingOpensAt() {
        return motion.getVotingOpensAt();
      }

      @Override
      public OffsetDateTime getVotingClosesAt() {
        return motion.getVotingClosesAt();
      }

      @Override
      public OffsetDateTime getCertificationOpensAt() {
        return motion.getCertificationOpensAt();
      }
    };
  }

  private <T> T withId(T entity) {
    if (ReflectionTestUtils.getField(entity, "id") == null) {
      ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
    return entity;
  }

  private long count(Iterable<?> values) {
    return StreamSupport.stream(values.spliterator(), false).count();
  }
}
