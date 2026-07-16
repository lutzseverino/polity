package com.odonta.polity.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.result.ActionUnavailableReason;
import com.odonta.polity.result.MotionActionAvailabilityResult;
import com.odonta.polity.service.MembershipService;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MotionActionAvailabilityResolverTest {
  private final AppealProposalRepository appeals = mock(AppealProposalRepository.class);
  private final ConstitutionalAuthority authority = mock(ConstitutionalAuthority.class);
  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final MembershipService memberships = mock(MembershipService.class);
  private final OfficeElectionCandidateRepository candidates =
      mock(OfficeElectionCandidateRepository.class);
  private final OfficeTermRepository terms = mock(OfficeTermRepository.class);
  private final MotionActionAvailabilityResolver resolver =
      new MotionActionAvailabilityResolver(
          Clock.systemUTC(), appeals, authority, powers, memberships, candidates, terms);

  @Test
  void disbandedPolityBlocksEveryMotionActionBeforeSecondaryReads() {
    UUID motionId = UUID.randomUUID();
    MotionProjection motion = mock(MotionProjection.class);
    when(motion.getId()).thenReturn(motionId);

    MotionActionAvailabilityResult result =
        resolver
            .resolveAll(
                UUID.randomUUID(),
                List.of(motion),
                Map.of(),
                PolityStatus.DISBANDED,
                null,
                Set.of())
            .get(motionId);

    assertThat(
            List.of(
                result.castVote(),
                result.castElectionBallot(),
                result.respondCandidacy(),
                result.requestCertification()))
        .allSatisfy(
            action -> {
              assertThat(action.available()).isFalse();
              assertThat(action.reason()).isEqualTo(ActionUnavailableReason.POLITY_DISBANDED);
            });
    verifyNoInteractions(appeals, authority, powers, memberships, candidates, terms);
  }
}
