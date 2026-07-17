package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.CertificationModality;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionBallot;
import com.odonta.polity.model.OfficeElectionBallotPreference;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProposalProjection;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.OfficeElectionProposalProjection;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class CertifyMotionWorkflowTest extends MotionWorkflowTestFixture {
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

    when(activeMemberships.resolve(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findEntityById(constitution.getId())).thenReturn(Optional.of(constitution));
    when(procedures.findEntityById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motionId)).thenReturn(3L);
    when(votes.findEntitiesByMotionId(motionId)).thenReturn(List.of(first, second), List.of());
    when(certifications.saveAndFlush(any(Certification.class)))
        .thenAnswer(invocation -> saveCertification(invocation.getArgument(0)));
    when(memberships.findEntityById(requesterMembershipId)).thenReturn(Optional.of(requester));
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));

    var result =
        certifyMotion.certify(
            polityId, motionId, new AuthenticatedUser(requesterUserId, "subject", "Requester"));

    assertThat(result.status()).isEqualTo(MotionStatus.ENACTED);
    assertThat(result.tally().eligible()).isEqualTo(3);
    assertThat(result.tally().yes()).isEqualTo(2);
    assertThat(result.tally().passed()).isTrue();
    assertThat(result.certification().modality()).isEqualTo(CertificationModality.YES_NO);
    assertThat(result.certification().eligibleCount()).isEqualTo(3);
    assertThat(result.certification().yesCount()).isEqualTo(2);
    assertThat(result.certification().noCount()).isZero();
    assertThat(result.certification().abstainCount()).isZero();
    assertThat(result.certification().quorumRequired()).isEqualTo(2);
    assertThat(result.certification().quorumMet()).isTrue();
    assertThat(result.certification().thresholdMet()).isTrue();
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

    when(activeMemberships.resolve(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findEntityById(constitution.getId())).thenReturn(Optional.of(constitution));

    assertThatThrownBy(
            () ->
                certifyMotion.certify(
                    polityId,
                    motionId,
                    new AuthenticatedUser(requesterUserId, "subject", "Requester")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This motion cannot be certified until voting closes.");
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
            VotingThreshold.OFFICE_ELECTION_RESULT);
    Office office = stewardOffice(polityId, constitution.getId(), motion.getJurisdictionId());
    UUID secondVoterMembershipId = UUID.randomUUID();
    List<OfficeElectionCandidate> acceptedCandidates =
        List.of(
            new OfficeElectionCandidate(polityId, motionId, winnerMembershipId),
            new OfficeElectionCandidate(polityId, motionId, otherCandidateMembershipId));

    when(activeMemberships.resolve(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findEntityById(constitution.getId())).thenReturn(Optional.of(constitution));
    when(procedures.findEntityById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motionId)).thenReturn(3L);
    when(officeElectionCandidates.findEntitiesByMotionIdAndStatus(
            motionId, OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(acceptedCandidates);
    when(officeElectionCandidates.findEntitiesByMotionId(motionId)).thenReturn(acceptedCandidates);
    when(memberships.findEntityById(winnerMembershipId)).thenReturn(Optional.of(winner));
    when(memberships.findEntityById(otherCandidateMembershipId))
        .thenReturn(Optional.of(otherCandidate));
    when(officeElectionBallotPreferences.findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(
            motionId))
        .thenReturn(
            List.of(
                preference(polityId, motionId, requesterMembershipId, winnerMembershipId, 1),
                preference(polityId, motionId, secondVoterMembershipId, winnerMembershipId, 1)));
    when(officeElectionProposals.findProjectedByMotionId(motionId))
        .thenReturn(
            Optional.of(
                projection(
                    OfficeElectionProposalProjection.class,
                    new com.odonta.polity.model.OfficeElectionProposal(
                        polityId,
                        motionId,
                        office.getId(),
                        1,
                        OfficeElectionMethod.RANKED_CHOICE))));
    when(offices.findEntityByIdAndPolityId(office.getId(), polityId))
        .thenReturn(Optional.of(office));
    when(certifications.saveAndFlush(any(Certification.class)))
        .thenAnswer(invocation -> saveCertification(invocation.getArgument(0)));
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));
    when(votes.findEntitiesByMotionId(motionId)).thenReturn(List.of());

    var result =
        certifyMotion.certify(
            polityId, motionId, new AuthenticatedUser(requesterUserId, "subject", "Requester"));

    assertThat(result.status()).isEqualTo(MotionStatus.ENACTED);
    assertThat(result.tally()).isNull();
    assertThat(result.electionTally().winners())
        .singleElement()
        .extracting(com.odonta.polity.model.OfficeElectionCandidateTallyResult::membershipId)
        .isEqualTo(winnerMembershipId);
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
    assertThat(certificationCaptor.getValue().getElectionWinnerCount()).isEqualTo(1);
    assertThat(certificationCaptor.getValue().getElectionTallySnapshot()).isNotNull();
    assertThat(certificationCaptor.getValue().getElectionTallySnapshot().winners())
        .singleElement()
        .extracting(com.odonta.polity.model.OfficeElectionCandidateTallyResult::membershipId)
        .isEqualTo(winnerMembershipId);
    verify(effects).apply(motion, requester, constitution, NOW);

    OfficeElectionBallot ballot =
        new OfficeElectionBallot(polityId, motionId, requesterMembershipId, NOW.minusHours(1));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, requesterUserId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(requester));
    when(memberships.findProjectedByPolityIdAndUserIdAndStatus(
            polityId, requesterUserId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(projection(MembershipProjection.class, requester)));
    when(officeElectionBallots.findEntityByMotionIdAndMembershipId(motionId, requesterMembershipId))
        .thenReturn(Optional.of(ballot));
    when(officeElectionBallots.findEntitiesByMotionId(motionId)).thenReturn(List.of(ballot));
    List<OfficeElectionBallotPreference> storedRanking =
        List.of(
            preference(polityId, motionId, requesterMembershipId, winnerMembershipId, 1),
            preference(polityId, motionId, requesterMembershipId, otherCandidateMembershipId, 2));
    when(officeElectionBallotPreferences.findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(
            motionId))
        .thenReturn(storedRanking);

    var storedResult = queryService.get(polityId, motionId, requesterUserId);

    assertThat(storedResult.electionTally().winners())
        .singleElement()
        .extracting(com.odonta.polity.model.OfficeElectionCandidateTallyResult::membershipId)
        .isEqualTo(winnerMembershipId);
    assertThat(storedResult.certification().modality())
        .isEqualTo(CertificationModality.OFFICE_ELECTION);
    assertThat(storedResult.certification().eligibleCount()).isEqualTo(3);
    assertThat(storedResult.certification().electionParticipationCount()).isEqualTo(2);
    assertThat(storedResult.certification().electionDecisive()).isTrue();
    assertThat(storedResult.certification().electionWinnerCount()).isEqualTo(1);
    assertThat(storedResult.officeElection().currentBallot().castAt()).isEqualTo(NOW.minusHours(1));
    assertThat(storedResult.officeElection().currentBallot().candidateMembershipIds())
        .containsExactly(winnerMembershipId, otherCandidateMembershipId);
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
            VotingThreshold.OFFICE_ELECTION_RESULT);
    Office office = stewardOffice(polityId, constitution.getId(), motion.getJurisdictionId());
    OfficeElectionCandidate disqualifiedCandidate =
        new OfficeElectionCandidate(polityId, motionId, disqualifiedMembershipId);
    OfficeElectionCandidate otherCandidateOption =
        new OfficeElectionCandidate(polityId, motionId, otherCandidateMembershipId);
    when(activeMemberships.resolve(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findEntityById(constitution.getId())).thenReturn(Optional.of(constitution));
    when(procedures.findEntityById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motionId)).thenReturn(3L);
    when(officeElectionCandidates.findEntitiesByMotionIdAndStatus(
            motionId, OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(List.of(disqualifiedCandidate, otherCandidateOption))
        .thenReturn(List.of(otherCandidateOption))
        .thenReturn(List.of(otherCandidateOption));
    when(memberships.findEntityById(disqualifiedMembershipId))
        .thenReturn(Optional.of(disqualified));
    when(memberships.findEntityById(otherCandidateMembershipId))
        .thenReturn(Optional.of(otherCandidate));
    when(membershipService.hasPoliticalStanding(disqualifiedMembershipId, NOW)).thenReturn(false);
    when(membershipService.hasPoliticalStanding(otherCandidateMembershipId, NOW)).thenReturn(true);
    when(officeElectionBallotPreferences.findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(
            motionId))
        .thenReturn(
            List.of(
                preference(
                    polityId, motionId, requesterMembershipId, disqualifiedMembershipId, 1)));
    when(officeElectionProposals.findProjectedByMotionId(motionId))
        .thenReturn(
            Optional.of(
                projection(
                    OfficeElectionProposalProjection.class,
                    new com.odonta.polity.model.OfficeElectionProposal(
                        polityId,
                        motionId,
                        office.getId(),
                        1,
                        OfficeElectionMethod.RANKED_CHOICE))));
    when(offices.findEntityByIdAndPolityId(office.getId(), polityId))
        .thenReturn(Optional.of(office));
    when(certifications.saveAndFlush(any(Certification.class)))
        .thenAnswer(invocation -> saveCertification(invocation.getArgument(0)));
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));
    when(votes.findEntitiesByMotionId(motionId)).thenReturn(List.of());

    var result =
        certifyMotion.certify(
            polityId, motionId, new AuthenticatedUser(requesterUserId, "subject", "Requester"));

    assertThat(disqualifiedCandidate.getStatus())
        .isEqualTo(OfficeElectionCandidateStatus.DISQUALIFIED);
    assertThat(result.status()).isEqualTo(MotionStatus.REJECTED);
    assertThat(result.electionTally().winners()).isEmpty();
    verify(officeElectionCandidates).saveAllAndFlush(List.of(disqualifiedCandidate));
    verify(effects, never()).apply(motion, requester, constitution, NOW);
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

    when(activeMemberships.resolve(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findEntityById(constitution.getId())).thenReturn(Optional.of(constitution));
    when(appealProposals.findProjectedByMotionId(motionId))
        .thenReturn(Optional.of(projection(AppealProposalProjection.class, proposal)));
    when(procedures.findEntityById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motionId)).thenReturn(1L);
    when(votes.findEntitiesByMotionId(motionId)).thenReturn(List.of(vote));
    when(certifications.saveAndFlush(any(Certification.class)))
        .thenAnswer(invocation -> saveCertification(invocation.getArgument(0)));
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));

    certifyMotion.certify(
        polityId, motionId, new AuthenticatedUser(requesterUserId, "subject", "Requester"));

    verify(authority).requireAppealCertification(requester, constitution);
    verify(authority, never()).require(requester, constitution, PowerCode.REQUEST_CERTIFICATION);
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

    when(activeMemberships.resolve(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findEntityById(constitution.getId())).thenReturn(Optional.of(constitution));

    assertThatThrownBy(
            () ->
                certifyMotion.certify(
                    polityId,
                    motionId,
                    new AuthenticatedUser(requesterUserId, "subject", "Requester")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This motion was introduced under a constitution that is no longer ratified.");
  }
}
