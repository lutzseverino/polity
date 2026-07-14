package com.odonta.polity.effect;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import java.time.OffsetDateTime;

interface MotionEffect {
  EffectType type();

  void apply(Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now);
}
