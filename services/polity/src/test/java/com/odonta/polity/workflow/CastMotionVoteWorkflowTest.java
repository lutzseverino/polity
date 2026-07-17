package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.CastVoteInput;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.VoteChoice;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CastMotionVoteWorkflowTest extends MotionWorkflowTestFixture {
  @Test
  void rejectsVotesFromMembersOutsideTheFrozenElectorate() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId, UUID.randomUUID());
    Motion motion = motion(polityId, motionId, member.getId());
    when(activeMemberships.resolve(polityId, userId)).thenReturn(member);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(electors.existsByMotionIdAndMembershipId(motionId, member.getId())).thenReturn(false);

    assertThatThrownBy(
            () ->
                castMotionVote.cast(
                    polityId,
                    motionId,
                    new AuthenticatedUser(userId, "subject", "Late Member"),
                    new CastVoteInput(VoteChoice.YES)))
        .isInstanceOf(ApiException.class)
        .hasMessage("This member was not eligible when voting opened.");
  }

  @Test
  void rejectsYesNoVotesOnOfficeElectionMotions() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID voterUserId = UUID.randomUUID();
    UUID voterMembershipId = UUID.randomUUID();
    Membership voter = member(polityId, voterUserId, voterMembershipId);
    Motion motion = motion(polityId, motionId, voterMembershipId, EffectType.ELECT_OFFICE);

    when(activeMemberships.resolve(polityId, voterUserId)).thenReturn(voter);
    when(motions.findEntityByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));

    assertThatThrownBy(
            () ->
                castMotionVote.cast(
                    polityId,
                    motionId,
                    new AuthenticatedUser(voterUserId, "subject", "Voter"),
                    new CastVoteInput(VoteChoice.YES)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Office elections require an election ballot, not a yes/no vote.");
  }
}
