package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.CreateOfficeElectionMotionInput;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.OfficeElectionProposalProjection;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

class IntroduceOfficeElectionMotionWorkflowTest extends MotionWorkflowTestFixture {
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
    Institution assembly = institution(polityId, jurisdiction.getId(), constitution.getId());
    Institution court =
        new Institution(
            polityId,
            jurisdiction.getId(),
            constitution.getId(),
            "Magistrates' Court",
            InstitutionKind.JUDICIARY);
    ReflectionTestUtils.setField(court, "id", UUID.randomUUID());
    Office office = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            EffectType.ELECT_OFFICE,
            VotingThreshold.OFFICE_ELECTION_RESULT);
    Motion[] saved = new Motion[1];

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(assembly));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.OFFICE_ELECTION))
        .thenReturn(Optional.of(procedure));
    when(offices.findEntityByIdAndPolityId(office.getId(), polityId))
        .thenReturn(Optional.of(office));
    when(memberships.findEntityById(candidateMembershipId)).thenReturn(Optional.of(candidate));
    when(membershipService.displayName(candidateMembershipId))
        .thenReturn(candidate.getDisplayName());
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(officeElectionProposals.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(officeElectionProposals.findProjectedByMotionId(any(UUID.class)))
        .thenAnswer(
            invocation ->
                Optional.of(
                    projection(
                        OfficeElectionProposalProjection.class,
                        new com.odonta.polity.model.OfficeElectionProposal(
                            polityId,
                            invocation.getArgument(0),
                            office.getId(),
                            1,
                            OfficeElectionMethod.RANKED_CHOICE))));
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, com.odonta.polity.model.MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor, candidate));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(electors.countByMotionId(any(UUID.class))).thenReturn(2L);
    when(votes.findEntitiesByMotionId(any(UUID.class))).thenReturn(List.of());
    when(officeElectionCandidates.findEntitiesByMotionIdAndStatus(
            any(UUID.class), eq(OfficeElectionCandidateStatus.ACCEPTED)))
        .thenReturn(List.of());
    when(officeElectionCandidates.findEntitiesByMotionId(any(UUID.class)))
        .thenAnswer(
            invocation ->
                List.of(
                    new OfficeElectionCandidate(
                        polityId,
                        invocation.getArgument(0),
                        candidateMembershipId,
                        OfficeElectionCandidateStatus.PENDING,
                        null)));
    when(officeElectionBallotPreferences.findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(
            any(UUID.class)))
        .thenReturn(List.of());

    var result =
        officeElectionMotions.introduce(
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
  void rejectsOfficeElectionCandidatesWithoutPoliticalStanding() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    UUID candidateMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    Membership candidate = member(polityId, UUID.randomUUID(), candidateMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution assembly = institution(polityId, jurisdiction.getId(), constitution.getId());
    Institution court =
        new Institution(
            polityId,
            jurisdiction.getId(),
            constitution.getId(),
            "Magistrates' Court",
            InstitutionKind.JUDICIARY);
    ReflectionTestUtils.setField(court, "id", UUID.randomUUID());
    Office office = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            EffectType.ELECT_OFFICE,
            VotingThreshold.OFFICE_ELECTION_RESULT);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(assembly));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.OFFICE_ELECTION))
        .thenReturn(Optional.of(procedure));
    when(offices.findEntityByIdAndPolityId(office.getId(), polityId))
        .thenReturn(Optional.of(office));
    when(memberships.findEntityById(candidateMembershipId)).thenReturn(Optional.of(candidate));
    doThrow(
            ApiException.forbidden(
                "political_standing_required",
                "This member lacks political standing for this constitutional action."))
        .when(membershipService)
        .requirePoliticalStanding(candidateMembershipId, NOW);

    assertThatThrownBy(
            () ->
                officeElectionMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateOfficeElectionMotionInput(
                        office.getId(), List.of(candidateMembershipId))))
        .isInstanceOf(ApiException.class)
        .hasMessage("This member lacks political standing for this constitutional action.");
  }
}
