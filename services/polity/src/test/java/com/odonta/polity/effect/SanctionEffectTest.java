package com.odonta.polity.effect;

import static com.odonta.polity.effect.EffectTestFixtures.NOW;
import static com.odonta.polity.effect.EffectTestFixtures.constitution;
import static com.odonta.polity.effect.EffectTestFixtures.member;
import static com.odonta.polity.effect.EffectTestFixtures.motion;
import static com.odonta.polity.effect.EffectTestFixtures.projection;
import static com.odonta.polity.effect.EffectTestFixtures.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionProposal;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.SanctionProposalProjection;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.service.OfficialRecordService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SanctionEffectTest {
  private final SanctionProposalRepository proposals = mock(SanctionProposalRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final SanctionEffect effect =
      new SanctionEffect(proposals, sanctions, memberships, officialRecords);

  @Test
  void persistsTheTimedSanction() {
    UUID polityId = UUID.randomUUID();
    var actor = member(polityId, "Actor");
    var target = member(polityId, "Target");
    var constitution = constitution(polityId);
    var motion = motion(polityId, actor, constitution, EffectType.APPLY_SANCTION);
    var proposal =
        new SanctionProposal(
            polityId, motion.getId(), target.getId(), SanctionType.SUSPENSION, "Reason", 14);
    when(proposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(Optional.of(projection(SanctionProposalProjection.class, proposal)));
    when(memberships.findEntityById(target.getId())).thenReturn(Optional.of(target));
    when(sanctions.saveAndFlush(any(Sanction.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    effect.apply(motion, actor, constitution, NOW);

    ArgumentCaptor<Sanction> saved = ArgumentCaptor.forClass(Sanction.class);
    verify(sanctions).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getTargetMembershipId()).isEqualTo(target.getId());
    assertThat(saved.getValue().getEndsAt()).isEqualTo(NOW.plusDays(14));
    verify(officialRecords).append(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }
}
