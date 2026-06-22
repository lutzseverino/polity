package com.odonta.polity.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConstitutionVersionTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  @Test
  void templateBackedConstitutionCarriesTranslationKeys() {
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            UUID.randomUUID(),
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackBody(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER,
            NOW);

    assertThat(constitution.getTitleKey())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.titleKey());
    assertThat(constitution.getBodyKey())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.bodyKey());
    assertThat(constitution.getTemplateParams()).isEmpty();
  }

  @Test
  void freeformConstitutionHasNoTranslationKeys() {
    ConstitutionVersion constitution =
        new ConstitutionVersion(UUID.randomUUID(), 2, "Custom Charter", "Body", NOW);

    assertThat(constitution.getTitleKey()).isNull();
    assertThat(constitution.getBodyKey()).isNull();
    assertThat(constitution.getTemplateParams()).isEmpty();
  }
}
