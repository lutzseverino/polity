package com.odonta.polity.effect;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

final class EffectTestFixtures {
  static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-14T10:00:00Z");

  private EffectTestFixtures() {}

  static Motion motion(
      UUID polityId, Membership actor, ConstitutionVersion constitution, EffectType type) {
    Motion motion =
        new Motion(
            polityId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            constitution.getId(),
            UUID.randomUUID(),
            actor.getId(),
            "Motion title",
            "Motion body",
            type,
            NOW.minusHours(1),
            NOW.minusHours(1),
            NOW.plusHours(1),
            NOW.plusHours(1));
    return withId(motion);
  }

  static ConstitutionVersion constitution(UUID polityId) {
    return withId(
        new ConstitutionVersion(polityId, 1, "Starter Constitution", "Body", NOW.minusDays(1)));
  }

  static Membership member(UUID polityId, String displayName) {
    return withId(
        new Membership(
            polityId,
            UUID.randomUUID(),
            "subject:" + UUID.randomUUID(),
            displayName.toLowerCase().replace(" ", ".") + "@example.com",
            displayName,
            NOW,
            null));
  }

  static <T> T withId(T entity) {
    ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    return entity;
  }

  static <T> T projection(Class<T> type, Object source) {
    return type.cast(
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (ignored, method, args) -> {
              if (method.getDeclaringClass() == Object.class) {
                return method.invoke(source, args);
              }
              return source.getClass().getMethod(method.getName()).invoke(source);
            }));
  }
}
