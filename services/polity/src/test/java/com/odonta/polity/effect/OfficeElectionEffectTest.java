package com.odonta.polity.effect;

import static com.odonta.polity.effect.EffectTestFixtures.NOW;
import static com.odonta.polity.effect.EffectTestFixtures.constitution;
import static com.odonta.polity.effect.EffectTestFixtures.member;
import static com.odonta.polity.effect.EffectTestFixtures.motion;
import static com.odonta.polity.effect.EffectTestFixtures.projection;
import static com.odonta.polity.effect.EffectTestFixtures.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.common.api.ApiException;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionBallotPreference;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.OfficeElectionProposal;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.OfficeElectionBallotPreferenceRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalProjection;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.service.MembershipService;
import com.odonta.polity.service.OfficialRecordService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OfficeElectionEffectTest {
  private final MotionElectorRepository electors = mock(MotionElectorRepository.class);
  private final OfficeElectionBallotPreferenceRepository preferences =
      mock(OfficeElectionBallotPreferenceRepository.class);
  private final OfficeElectionCandidateRepository candidates =
      mock(OfficeElectionCandidateRepository.class);
  private final OfficeElectionProposalRepository proposals =
      mock(OfficeElectionProposalRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final MembershipService membershipService = mock(MembershipService.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final OfficeElectionEffect effect =
      new OfficeElectionEffect(
          electors,
          preferences,
          candidates,
          new OfficeElectionEvaluator(),
          proposals,
          offices,
          officeTerms,
          procedures,
          memberships,
          membershipService,
          officialRecords);

  @Test
  void batchesCandidateStateAndLoadsProposalAndOfficeOnlyOnce() {
    UUID polityId = UUID.randomUUID();
    UUID winnerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    var actor = member(polityId, "Actor");
    var constitution = constitution(polityId);
    var motion = motion(polityId, actor, constitution, EffectType.ELECT_OFFICE);
    Office office =
        withId(
            new Office(
                polityId,
                constitution.getId(),
                motion.getJurisdictionId(),
                Office.STEWARD,
                "Steward",
                "Coordinates",
                14));
    Procedure procedure =
        new Procedure(
            polityId,
            constitution.getId(),
            motion.getInstitutionId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            1,
            2,
            VotingThreshold.OFFICE_ELECTION_RESULT,
            0,
            24,
            EffectType.ELECT_OFFICE);
    org.springframework.test.util.ReflectionTestUtils.setField(
        procedure, "id", motion.getProcedureId());
    OfficeElectionProposal proposal =
        new OfficeElectionProposal(
            polityId, motion.getId(), office.getId(), 1, OfficeElectionMethod.RANKED_CHOICE);
    MembershipProjection winner = membershipProjection(winnerId, "Winner");
    MembershipProjection other = membershipProjection(otherId, "Other");
    UUID secondVoterId = UUID.randomUUID();

    when(proposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(Optional.of(projection(OfficeElectionProposalProjection.class, proposal)));
    when(offices.findEntityByIdAndPolityId(office.getId(), polityId))
        .thenReturn(Optional.of(office));
    when(procedures.findEntityById(motion.getProcedureId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motion.getId())).thenReturn(3L);
    when(candidates.findEntitiesByMotionIdAndStatus(
            motion.getId(), OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(
            List.of(
                new OfficeElectionCandidate(polityId, motion.getId(), winnerId),
                new OfficeElectionCandidate(polityId, motion.getId(), otherId)));
    when(memberships.findProjectionsByPolityIdAndIdIn(eq(polityId), any()))
        .thenReturn(List.of(winner, other));
    when(membershipService.politicalStanding(eq(polityId), any(), eq(NOW)))
        .thenReturn(Set.of(winnerId, otherId));
    when(officeTerms.findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
            polityId, office.getCode(), OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(List.of());
    when(preferences.findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(motion.getId()))
        .thenReturn(
            List.of(
                preference(polityId, motion.getId(), actor.getId(), winnerId, 1),
                preference(polityId, motion.getId(), secondVoterId, winnerId, 1)));
    when(officeTerms.countByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, office.getCode(), OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(0L);
    when(officeTerms.saveAndFlush(any(OfficeTerm.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    effect.apply(motion, actor, constitution, NOW);

    ArgumentCaptor<OfficeTerm> saved = ArgumentCaptor.forClass(OfficeTerm.class);
    verify(officeTerms).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getMembershipId()).isEqualTo(winnerId);
    verify(proposals, times(1)).findProjectedByMotionId(motion.getId());
    verify(offices, times(1)).findEntityByIdAndPolityId(office.getId(), polityId);
    verify(memberships, times(1)).findProjectionsByPolityIdAndIdIn(eq(polityId), any());
    verify(membershipService, times(1)).politicalStanding(eq(polityId), any(), eq(NOW));
    verify(officeTerms, times(1))
        .findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
            polityId, office.getCode(), OfficeTermStatus.ACTIVE, NOW);
  }

  @Test
  void rejectsAnElectionWhenTheOfficeHasNoVacantSeat() {
    UUID polityId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    var actor = member(polityId, "Actor");
    var constitution = constitution(polityId);
    var motion = motion(polityId, actor, constitution, EffectType.ELECT_OFFICE);
    Office office =
        withId(
            new Office(
                polityId,
                constitution.getId(),
                motion.getJurisdictionId(),
                Office.STEWARD,
                "Steward",
                "Coordinates",
                14));
    Procedure procedure =
        new Procedure(
            polityId,
            constitution.getId(),
            motion.getInstitutionId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            1,
            2,
            VotingThreshold.OFFICE_ELECTION_RESULT,
            0,
            24,
            EffectType.ELECT_OFFICE);
    org.springframework.test.util.ReflectionTestUtils.setField(
        procedure, "id", motion.getProcedureId());
    OfficeElectionProposal proposal =
        new OfficeElectionProposal(
            polityId, motion.getId(), office.getId(), 1, OfficeElectionMethod.RANKED_CHOICE);
    MembershipProjection candidate = membershipProjection(candidateId, "Candidate");
    when(proposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(Optional.of(projection(OfficeElectionProposalProjection.class, proposal)));
    when(offices.findEntityByIdAndPolityId(office.getId(), polityId))
        .thenReturn(Optional.of(office));
    when(procedures.findEntityById(motion.getProcedureId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motion.getId())).thenReturn(1L);
    when(candidates.findEntitiesByMotionIdAndStatus(
            motion.getId(), OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(List.of(new OfficeElectionCandidate(polityId, motion.getId(), candidateId)));
    when(memberships.findProjectionsByPolityIdAndIdIn(eq(polityId), any()))
        .thenReturn(List.of(candidate));
    when(membershipService.politicalStanding(eq(polityId), any(), eq(NOW)))
        .thenReturn(Set.of(candidateId));
    when(officeTerms.findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
            polityId, office.getCode(), OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(List.of());
    when(preferences.findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(motion.getId()))
        .thenReturn(List.of(preference(polityId, motion.getId(), actor.getId(), candidateId, 1)));
    when(officeTerms.countByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, office.getCode(), OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(1L);

    assertThatThrownBy(() -> effect.apply(motion, actor, constitution, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("no vacant seats");

    verify(officeTerms, never()).saveAndFlush(any());
  }

  private MembershipProjection membershipProjection(UUID id, String name) {
    MembershipProjection projection = mock(MembershipProjection.class);
    when(projection.getId()).thenReturn(id);
    when(projection.getDisplayName()).thenReturn(name);
    return projection;
  }

  private OfficeElectionBallotPreference preference(
      UUID polityId, UUID motionId, UUID voterId, UUID candidateId, int rank) {
    return new OfficeElectionBallotPreference(
        polityId, motionId, UUID.randomUUID(), voterId, candidateId, rank);
  }
}
