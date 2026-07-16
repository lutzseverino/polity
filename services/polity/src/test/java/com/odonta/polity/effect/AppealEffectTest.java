package com.odonta.polity.effect;

import static com.odonta.polity.effect.EffectTestFixtures.NOW;
import static com.odonta.polity.effect.EffectTestFixtures.constitution;
import static com.odonta.polity.effect.EffectTestFixtures.member;
import static com.odonta.polity.effect.EffectTestFixtures.motion;
import static com.odonta.polity.effect.EffectTestFixtures.projection;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.repository.AppealProposalProjection;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.service.OfficialRecordService;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppealEffectTest {
  private final AppealProposalRepository proposals = mock(AppealProposalRepository.class);
  private final AppealRepository appeals = mock(AppealRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final AppealEffect effect =
      new AppealEffect(proposals, appeals, sanctions, memberships, officialRecords);

  @Test
  void rejectsAnExpiredSanctionAtCertificationTime() {
    UUID polityId = UUID.randomUUID();
    var actor = member(polityId, "Actor");
    var constitution = constitution(polityId);
    var motion = motion(polityId, actor, constitution, EffectType.GRANT_APPEAL);
    UUID sanctionId = UUID.randomUUID();
    Sanction sanction =
        new Sanction(
            polityId,
            UUID.randomUUID(),
            actor.getId(),
            SanctionType.SUSPENSION,
            "Reason",
            NOW.minusDays(2),
            NOW.minusDays(1));
    org.springframework.test.util.ReflectionTestUtils.setField(sanction, "id", sanctionId);
    var proposal =
        new AppealProposal(polityId, motion.getId(), sanctionId, actor.getId(), "Appeal reason");
    when(proposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(Optional.of(projection(AppealProposalProjection.class, proposal)));
    when(sanctions.findEntityByIdAndPolityId(proposal.getSanctionId(), polityId))
        .thenReturn(Optional.of(sanction));

    assertThatThrownBy(() -> effect.apply(motion, actor, constitution, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("active sanctions");
  }
}
