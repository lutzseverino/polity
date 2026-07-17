package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.RespondOfficeElectionCandidacyInput;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.VotingThreshold;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RespondOfficeElectionCandidacyWorkflowTest extends MotionWorkflowTestFixture {
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
            VotingThreshold.OFFICE_ELECTION_RESULT);

    when(activeMemberships.resolve(polityId, candidateUserId)).thenReturn(candidate);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(officeElectionCandidates.findEntityByMotionIdAndMembershipId(
            motionId, candidateMembershipId))
        .thenReturn(Optional.of(candidacy));
    when(officeElectionCandidates.saveAndFlush(any(OfficeElectionCandidate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));
    when(electors.countByMotionId(motionId)).thenReturn(1L);
    when(votes.findEntitiesByMotionId(motionId)).thenReturn(List.of());
    when(officeElectionCandidates.findEntitiesByMotionIdAndStatus(
            motionId, OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(List.of(candidacy));
    when(officeElectionBallotPreferences.findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(
            motionId))
        .thenReturn(List.of());
    when(memberships.findEntityById(candidateMembershipId)).thenReturn(Optional.of(candidate));

    respondOfficeElectionCandidacy.respond(
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

    when(activeMemberships.resolve(polityId, candidateUserId)).thenReturn(candidate);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));

    assertThatThrownBy(
            () ->
                respondOfficeElectionCandidacy.respond(
                    polityId,
                    motionId,
                    new AuthenticatedUser(candidateUserId, "subject", "Candidate"),
                    new RespondOfficeElectionCandidacyInput(true)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Candidate responses close when voting opens.");

    verify(officeElectionCandidates, never()).saveAndFlush(any());
  }
}
