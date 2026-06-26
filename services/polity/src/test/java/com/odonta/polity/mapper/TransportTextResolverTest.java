package com.odonta.polity.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

class TransportTextResolverTest {

  @Test
  void resolvesMessageWithNestedKeyedParameter() {
    try {
      LocaleContextHolder.setLocale(Locale.ENGLISH);
      StaticMessageSource messages = new StaticMessageSource();
      messages.addMessage(
          "official_record.motion_certified.title",
          Locale.ENGLISH,
          "Result certified: {motionTitle}");
      messages.addMessage("motion.sanction.title", Locale.ENGLISH, "Sanction {targetName}");
      TransportTextResolver resolver = new TransportTextResolver(messages);

      String text =
          resolver.resolve(
              "official_record.motion_certified.title",
              "official_record.motion_certified.title",
              Map.of(
                  "motionTitle",
                  "motion.sanction.title",
                  "motionTitleKey",
                  "motion.sanction.title",
                  "targetName",
                  "Ada"));

      assertThat(text).isEqualTo("Result certified: Sanction Ada");
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }
}
