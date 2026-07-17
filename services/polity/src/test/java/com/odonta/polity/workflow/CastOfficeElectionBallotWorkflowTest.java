package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.CastOfficeElectionBallotInput;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CastOfficeElectionBallotWorkflowTest extends MotionWorkflowTestFixture {
  @Test
  void rejectsElectionBallotsForPendingCandidates() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID voterUserId = UUID.randomUUID();
    UUID voterMembershipId = UUID.randomUUID();
    UUID candidateMembershipId = UUID.randomUUID();
    Membership voter = member(polityId, voterUserId, voterMembershipId);
    Motion motion = motion(polityId, motionId, voterMembershipId, EffectType.ELECT_OFFICE);

    when(activeMemberships.resolve(polityId, voterUserId)).thenReturn(voter);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(electors.existsByMotionIdAndMembershipId(motionId, voterMembershipId)).thenReturn(true);
    when(officeElectionCandidates.existsByMotionIdAndMembershipIdAndStatus(
            motionId, candidateMembershipId, OfficeElectionCandidateStatus.ACCEPTED))
        .thenReturn(false);

    assertThatThrownBy(
            () ->
                castOfficeElectionBallot.cast(
                    polityId,
                    motionId,
                    new AuthenticatedUser(voterUserId, "subject", "Voter"),
                    new CastOfficeElectionBallotInput(List.of(candidateMembershipId))))
        .isInstanceOf(ApiException.class)
        .hasMessage("This member is not an accepted candidate in the election.");
  }
}
