package com.odonta.polity.effect;

import static com.odonta.polity.effect.EffectTestFixtures.NOW;
import static com.odonta.polity.effect.EffectTestFixtures.constitution;
import static com.odonta.polity.effect.EffectTestFixtures.member;
import static com.odonta.polity.effect.EffectTestFixtures.motion;
import static com.odonta.polity.effect.EffectTestFixtures.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Resolution;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.service.OfficialRecordService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResolutionAdoptionEffectTest {
  private final ResolutionRepository resolutions = mock(ResolutionRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final ResolutionAdoptionEffect effect =
      new ResolutionAdoptionEffect(resolutions, officialRecords);

  @Test
  void persistsTheAdoptedResolution() {
    UUID polityId = UUID.randomUUID();
    var actor = member(polityId, "Actor");
    var constitution = constitution(polityId);
    var motion = motion(polityId, actor, constitution, EffectType.ADOPT_RESOLUTION);
    when(resolutions.saveAndFlush(any(Resolution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    effect.apply(motion, actor, constitution, NOW);

    ArgumentCaptor<Resolution> saved = ArgumentCaptor.forClass(Resolution.class);
    verify(resolutions).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getMotionId()).isEqualTo(motion.getId());
    assertThat(saved.getValue().getAdoptedAt()).isEqualTo(NOW);
    verify(officialRecords).append(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }
}
