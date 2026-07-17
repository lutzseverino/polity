package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionElector;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.result.ActionUnavailableReason;
import com.odonta.polity.workflow.MotionReadTestFixture;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MotionServiceTest {
  private final MotionReadTestFixture fixture = new MotionReadTestFixture();

  @Test
  void motionResultExposesCurrentVoteAndExecutableActions() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID membershipId = UUID.randomUUID();
    Membership member = fixture.member(polityId, userId, membershipId);
    Motion motion = fixture.motion(polityId, motionId, membershipId);
    Procedure procedure =
        fixture.procedure(polityId, motion.getProcedureId(), motion.getConstitutionVersionId());
    Vote vote =
        new Vote(polityId, motionId, membershipId, VoteChoice.YES, fixture.now().minusMinutes(5));

    MembershipProjection membershipProjection = mock(MembershipProjection.class);
    when(membershipProjection.getId()).thenReturn(membershipId);

    when(fixture
            .memberships()
            .findProjectedByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(membershipProjection));
    when(fixture.memberships().findEntityById(membershipId)).thenReturn(Optional.of(member));
    MotionProjection motionProjection = fixture.projection(motion, procedure);
    when(fixture.motions().findProjectedByIdAndPolityId(motionId, polityId))
        .thenReturn(Optional.of(motionProjection));
    when(fixture.electors().countByMotionId(motionId)).thenReturn(1L);
    when(fixture.electors().existsByMotionIdAndMembershipId(motionId, membershipId))
        .thenReturn(true);
    when(fixture.electors().findEntitiesByMotionId(motionId))
        .thenReturn(List.of(new MotionElector(polityId, motionId, membershipId)));
    when(fixture.votes().findEntitiesByMotionId(motionId)).thenReturn(List.of(vote));
    when(fixture.votes().findEntityByMotionIdAndMembershipId(motionId, membershipId))
        .thenReturn(Optional.of(vote));

    var result = fixture.service().get(polityId, motionId, userId);

    assertThat(result.currentVote()).isEqualTo(VoteChoice.YES);
    assertThat(result.actions().castVote().available()).isTrue();
    assertThat(result.actions().castElectionBallot().available()).isFalse();
    assertThat(result.actions().castElectionBallot().reason())
        .isEqualTo(ActionUnavailableReason.MOTION_NOT_OFFICE_ELECTION);
    assertThat(result.actions().respondCandidacy().available()).isFalse();
    assertThat(result.actions().requestCertification().available()).isFalse();
    assertThat(result.actions().requestCertification().reason())
        .isEqualTo(ActionUnavailableReason.CERTIFICATION_NOT_OPEN);
  }
}
