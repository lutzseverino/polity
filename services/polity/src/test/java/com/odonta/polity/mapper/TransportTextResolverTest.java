package com.odonta.polity.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.result.OfficeTermResult;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
      TransportTextResolver resolver = resolver(messages);

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

  @Test
  void resolvesConstitutionChangeSummaryFromStructuredItems() {
    try {
      LocaleContextHolder.setLocale(Locale.ENGLISH);
      StaticMessageSource messages = new StaticMessageSource();
      messages.addMessage(
          "official_record.constitution_amended.body", Locale.ENGLISH, "Changes: {changeSummary}");
      messages.addMessage(
          "official_record.constitution_amended.change_item",
          Locale.ENGLISH,
          "{operation} {kind} {subject}{details}");
      messages.addMessage(
          "official_record.constitution_amended.change_detail", Locale.ENGLISH, "{name}: {value}");
      messages.addMessage(
          "official_record.constitution_amended.change_detail_prefix", Locale.ENGLISH, " - ");
      messages.addMessage(
          "official_record.constitution_amended.change_detail_separator", Locale.ENGLISH, ", ");
      messages.addMessage("constitution_change.kind.procedure", Locale.ENGLISH, "procedure");
      messages.addMessage(
          "procedure.ordinary_resolution.name", Locale.ENGLISH, "Ordinary resolution");
      messages.addMessage("constitution_change.detail.threshold", Locale.ENGLISH, "threshold");
      messages.addMessage(
          "constitution_change.detail.minimumNoticeHours", Locale.ENGLISH, "minimum notice hours");
      messages.addMessage("constitution_change.operation.revise", Locale.ENGLISH, "revised");
      messages.addMessage(
          "constitution_change.value.two_thirds_cast", Locale.ENGLISH, "two thirds of votes cast");
      TransportTextResolver resolver = resolver(messages);
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("minimumNoticeHours", 12);
      details.put("threshold", VotingThreshold.TWO_THIRDS_CAST);

      String text =
          resolver.resolve(
              "official_record.constitution_amended.body",
              "official_record.constitution_amended.body",
              Map.of(
                  "changeItems",
                  java.util.List.of(
                      Map.of(
                          "kind",
                          "procedure",
                          "operation",
                          "revise",
                          "subject",
                          "ordinary-resolution",
                          "subjectKey",
                          "procedure.ordinary_resolution.name",
                          "details",
                          details))));

      assertThat(text)
          .isEqualTo(
              "Changes: revised procedure Ordinary resolution - minimum notice hours: 12, threshold: two thirds of votes cast");
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test
  void resolvesOfficeTermOfficeName() {
    try {
      LocaleContextHolder.setLocale(Locale.ENGLISH);
      StaticMessageSource messages = new StaticMessageSource();
      messages.addMessage("office.magistrate.name", Locale.ENGLISH, "Magistrate");
      TransportTextResolver resolver = resolver(messages);
      OfficeTermTransportText text = new OfficeTermTransportText(resolver);
      OfficeTermResult result =
          new OfficeTermResult(
              UUID.randomUUID(),
              UUID.randomUUID(),
              "office.magistrate.name",
              "office.magistrate.name",
              UUID.randomUUID(),
              "Ada",
              OfficeTermStatus.ACTIVE,
              null,
              null);

      assertThat(text.officeName(result)).isEqualTo("Magistrate");
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  private TransportTextResolver resolver(StaticMessageSource messages) {
    return new TransportTextResolver(new ConstitutionChangeTextResolver(messages), messages);
  }
}
