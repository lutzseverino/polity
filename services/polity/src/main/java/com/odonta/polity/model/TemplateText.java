package com.odonta.polity.model;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

final class TemplateText {
  private TemplateText() {}

  static String render(String template, Map<String, ?> params) {
    String rendered = template;
    for (Map.Entry<String, ?> entry : params.entrySet()) {
      rendered = rendered.replace("{" + entry.getKey() + "}", value(entry.getValue()));
    }
    return rendered;
  }

  private static String value(Object value) {
    if (value instanceof Iterable<?> iterable) {
      return StreamSupport.stream(iterable.spliterator(), false)
          .map(String::valueOf)
          .collect(Collectors.joining(", "));
    }
    return String.valueOf(value);
  }
}
