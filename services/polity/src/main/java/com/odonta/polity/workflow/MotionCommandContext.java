package com.odonta.polity.workflow;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.resolver.ActiveMembershipResolver;
import com.odonta.polity.resolver.MotionResultResolver;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.service.PolityService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class MotionCommandContext {
  private final Clock clock;
  private final ActiveMembershipResolver activeMemberships;
  private final MotionRepository motions;
  private final PolityService polities;
  private final MotionResultResolver results;

  OffsetDateTime now() {
    return OffsetDateTime.now(clock);
  }

  Membership activeMember(UUID polityId, AuthenticatedUser actor) {
    polities.requireActive(polityId);
    return activeMemberships.resolve(polityId, actor.id());
  }

  Motion motion(UUID polityId, UUID motionId) {
    return motions
        .findEntityByIdAndPolityId(motionId, polityId)
        .orElseThrow(PolityResource.MOTION::notFound);
  }

  MotionResult result(Motion motion, UUID membershipId) {
    return results.resolve(motion, membershipId);
  }

  void requireVoting(Motion motion, OffsetDateTime now) {
    requireVotingStatus(motion);
    if (now.isBefore(motion.getVotingOpensAt())) {
      throw ApiException.conflict("voting_not_open", "Voting has not opened for this motion.");
    }
    if (!now.isBefore(motion.getVotingClosesAt())) {
      throw ApiException.conflict("voting_closed", "Voting has closed for this motion.");
    }
  }

  void requireCandidacyResponseOpen(Motion motion, OffsetDateTime now) {
    requireVotingStatus(motion);
    if (!now.isBefore(motion.getVotingOpensAt())) {
      throw ApiException.conflict(
          "candidacy_response_closed", "Candidate responses close when voting opens.");
    }
  }

  void requireVotingStatus(Motion motion) {
    if (motion.getStatus() != MotionStatus.VOTING) {
      throw ApiException.conflict("motion_not_voting", "This motion is no longer open for voting.");
    }
  }
}
