package com.odonta.polity.mapper;

import com.odonta.polity.model.TemplateText;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransportTextResolver {
  private static final String KEY_SUFFIX = "Key";

  private final MessageSource messages;

  public String resolve(String key, String fallback, Map<String, ?> params) {
    if (key == null || key.isBlank()) {
      return fallback;
    }
    String template = message(key, fallback == null || fallback.isBlank() ? key : fallback);
    return TemplateText.render(template, resolvedParams(params));
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
            resolved.put(resolvedName, TemplateText.render(template, resolved));
          }
        });
    return resolved;
  }

  private String message(String key, String fallback) {
    return messages.getMessage(key, null, fallback, LocaleContextHolder.getLocale());
  }
}
