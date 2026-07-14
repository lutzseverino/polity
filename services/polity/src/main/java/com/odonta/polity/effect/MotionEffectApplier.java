package com.odonta.polity.effect;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MotionEffectApplier {
  private final Map<EffectType, MotionEffect> effects;

  public MotionEffectApplier(List<MotionEffect> effects) {
    EnumMap<EffectType, MotionEffect> effectsByType = new EnumMap<>(EffectType.class);
    effects.forEach(
        effect -> {
          MotionEffect previous = effectsByType.put(effect.type(), effect);
          if (previous != null) {
            throw new IllegalStateException("Multiple motion effects handle " + effect.type());
          }
        });
    if (!effectsByType.keySet().equals(EnumSet.allOf(EffectType.class))) {
      EnumSet<EffectType> missing = EnumSet.allOf(EffectType.class);
      missing.removeAll(effectsByType.keySet());
      throw new IllegalStateException("No motion effect handles " + missing);
    }
    this.effects = Map.copyOf(effectsByType);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    MotionEffect effect = effects.get(motion.getEffectType());
    if (effect == null) {
      throw new IllegalStateException("No motion effect handles " + motion.getEffectType());
    }
    effect.apply(motion, actor, constitution, now);
  }
}
