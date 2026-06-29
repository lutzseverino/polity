package com.odonta.polity.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransportTextResolver {
  private static final String CHANGE_ITEMS = "changeItems";
  private static final String CHANGE_SUMMARY = "changeSummary";
  private static final String KEY_SUFFIX = "Key";

  private final ConstitutionChangeTextResolver constitutionChanges;
  private final MessageSource messages;

  public String resolve(String key, String fallback, Map<String, ?> params) {
    if (key == null || key.isBlank()) {
      return fallback;
    }
    String template = message(key, fallback == null || fallback.isBlank() ? key : fallback);
    return render(template, resolvedParams(params));
  }

  public String resolveName(String key, String fallback) {
    return resolve(key, fallback, Map.of());
  }

  private Map<String, Object> resolvedParams(Map<String, ?> params) {
    if (params == null || params.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> resolved = new LinkedHashMap<>(params);
    params.forEach(
        (name, value) -> {
          if (name.endsWith(KEY_SUFFIX) && value instanceof String key) {
            String resolvedName = name.substring(0, name.length() - KEY_SUFFIX.length());
            Object fallback = resolved.get(resolvedName);
            String template = message(key, fallback == null ? key : String.valueOf(fallback));
            resolved.put(resolvedName, render(template, resolved));
          }
        });
    resolved.putIfAbsent(
        CHANGE_SUMMARY, constitutionChanges.changeSummary(params.get(CHANGE_ITEMS)));
    return resolved;
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
