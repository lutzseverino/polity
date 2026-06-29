package com.odonta.polity.mapper;

import com.odonta.polity.model.ConstitutionalPowerTemplateKey;
import com.odonta.polity.model.PowerCode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ConstitutionChangeTextResolver {
  private static final String CONSTITUTION_CHANGE_ITEM =
      "official_record.constitution_amended.change_item";
  private static final String CONSTITUTION_CHANGE_DETAIL =
      "official_record.constitution_amended.change_detail";
  private static final String CONSTITUTION_CHANGE_DETAIL_PREFIX =
      "official_record.constitution_amended.change_detail_prefix";
  private static final String CONSTITUTION_CHANGE_DETAIL_SEPARATOR =
      "official_record.constitution_amended.change_detail_separator";
  private static final String CONSTITUTION_CHANGE_SEPARATOR =
      "official_record.constitution_amended.change_separator";
  private static final String DETAIL_PREFIX = "constitution_change.detail.";
  private static final String KIND_PREFIX = "constitution_change.kind.";
  private static final String OPERATION_PREFIX = "constitution_change.operation.";
  private static final String VALUE_PREFIX = "constitution_change.value.";
  private static final String KEY_SUFFIX = "Key";

  private final MessageSource messages;

  String changeSummary(Object value) {
    if (!(value instanceof Iterable<?> iterable)) {
      return "";
    }
    String separator = message(CONSTITUTION_CHANGE_SEPARATOR, "; ");
    return StreamSupport.stream(iterable.spliterator(), false)
        .map(this::changeItem)
        .collect(Collectors.joining(separator));
  }

  private String changeItem(Object value) {
    if (!(value instanceof Map<?, ?> item)) {
      return String.valueOf(value);
    }
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("kind", changeLabel(KIND_PREFIX, item.get("kind")));
    params.put("operation", changeLabel(OPERATION_PREFIX, item.get("operation")));
    params.put("subject", keyedValue(item, "subject"));
    params.put("details", detailSummary(item.get("details")));
    return render(
        message(CONSTITUTION_CHANGE_ITEM, "{operation} {kind} {subject}{details}"), params);
  }

  private String detailSummary(Object value) {
    if (!(value instanceof Map<?, ?> details) || details.isEmpty()) {
      return "";
    }
    String separator = message(CONSTITUTION_CHANGE_DETAIL_SEPARATOR, ", ");
    String summary =
        details.entrySet().stream()
            .map(entry -> detail(String.valueOf(entry.getKey()), entry.getValue()))
            .collect(Collectors.joining(separator));
    return message(CONSTITUTION_CHANGE_DETAIL_PREFIX, " - ") + summary;
  }

  private String detail(String name, Object value) {
    return render(
        message(CONSTITUTION_CHANGE_DETAIL, "{name}: {value}"),
        Map.of("name", changeLabel(DETAIL_PREFIX, name), "value", changeValue(value)));
  }

  private String changeLabel(String prefix, Object value) {
    String raw = value == null ? "" : String.valueOf(value);
    return message(prefix + raw, raw);
  }

  private String changeValue(Object value) {
    if (value instanceof Map<?, ?> keyedValue && keyedValue.containsKey("value")) {
      return keyedValue(keyedValue, "value");
    }
    if (value instanceof PowerCode powerCode) {
      return message(
          ConstitutionalPowerTemplateKey.valueOf(powerCode.name()).nameKey(),
          powerCode.name().toLowerCase(Locale.ROOT));
    }
    if (value instanceof Enum<?> enumValue) {
      String raw = enumValue.name().toLowerCase(Locale.ROOT);
      return message(VALUE_PREFIX + raw, raw);
    }
    return this.value(value);
  }

  private String keyedValue(Map<?, ?> values, String name) {
    Object value = values.get(name);
    Object key = values.get(name + KEY_SUFFIX);
    if (key instanceof String messageKey && !messageKey.isBlank()) {
      return message(messageKey, value == null ? messageKey : String.valueOf(value));
    }
    return value(value);
  }

  private String render(String template, Map<String, ?> params) {
    String rendered = template;
    for (Map.Entry<String, ?> entry : params.entrySet()) {
      rendered = rendered.replace("{" + entry.getKey() + "}", value(entry.getValue()));
    }
    return rendered;
  }

  private String value(Object value) {
    if (value instanceof Iterable<?> iterable) {
      return StreamSupport.stream(iterable.spliterator(), false)
          .map(String::valueOf)
          .collect(Collectors.joining(", "));
    }
    return String.valueOf(value);
  }

  private String message(String key, String fallback) {
    return messages.getMessage(key, null, fallback, LocaleContextHolder.getLocale());
  }
}
