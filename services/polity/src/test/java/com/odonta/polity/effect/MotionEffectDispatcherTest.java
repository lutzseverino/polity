package com.odonta.polity.effect;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MotionEffectDispatcherTest {
  @Test
  void dispatchesToTheEffectRegisteredForTheMotionType() {
    List<MotionEffect> effects = completeEffects();
    MotionEffect selected = effects.get(EffectType.APPLY_SANCTION.ordinal());
    Motion motion = mock(Motion.class);
    Membership actor = mock(Membership.class);
    ConstitutionVersion constitution = mock(ConstitutionVersion.class);
    OffsetDateTime now = OffsetDateTime.parse("2026-07-12T10:00:00Z");
    when(motion.getEffectType()).thenReturn(EffectType.APPLY_SANCTION);

    new MotionEffectApplier(effects).apply(motion, actor, constitution, now);

    verify(selected).apply(motion, actor, constitution, now);
  }

  @Test
  void rejectsIncompleteEffectRegistrationAtConstructionTime() {
    List<MotionEffect> effects = completeEffects();

    assertThatThrownBy(() -> new MotionEffectApplier(effects.subList(1, effects.size())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(EffectType.ADOPT_RESOLUTION.name());
  }

  @Test
  void rejectsDuplicateEffectRegistrationAtConstructionTime() {
    List<MotionEffect> effects = completeEffects();

    assertThatThrownBy(
            () ->
                new MotionEffectApplier(
                    java.util.stream.Stream.concat(
                            effects.stream(), java.util.stream.Stream.of(effects.getFirst()))
                        .toList()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(EffectType.ADOPT_RESOLUTION.name());
  }

  private List<MotionEffect> completeEffects() {
    return Arrays.stream(EffectType.values())
        .map(
            type -> {
              MotionEffect effect = mock(MotionEffect.class);
              when(effect.type()).thenReturn(type);
              return effect;
            })
        .toList();
  }
}
