package com.odonta.polity.workflow;

import com.odonta.polity.model.Membership;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.resolver.ActiveMembershipResolver;
import com.odonta.polity.service.MembershipService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OfficeElectionCandidateEligibilityApplicator {
  private final ActiveMembershipResolver activeMemberships;
  private final OfficeElectionCandidateRepository candidates;
  private final MembershipService memberships;

  void disqualifyWithoutStanding(UUID motionId, OffsetDateTime now) {
    List<OfficeElectionCandidate> disqualified =
        candidates
            .findEntitiesByMotionIdAndStatus(motionId, OfficeElectionCandidateStatus.ACCEPTED)
            .stream()
            .filter(candidate -> !hasStanding(candidate, now))
            .toList();
    disqualified.forEach(candidate -> candidate.disqualify(now));
    if (!disqualified.isEmpty()) {
      candidates.saveAllAndFlush(disqualified);
    }
  }

  private boolean hasStanding(OfficeElectionCandidate candidate, OffsetDateTime now) {
    Membership membership =
        activeMemberships.resolveById(candidate.getPolityId(), candidate.getMembershipId());
    return memberships.hasPoliticalStanding(membership.getId(), now);
  }
}
