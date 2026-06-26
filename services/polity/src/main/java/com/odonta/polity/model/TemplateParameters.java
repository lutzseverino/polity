package com.odonta.polity.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TemplateParameters {
  private TemplateParameters() {}

  public static Map<String, Object> empty() {
    return Map.of();
  }

  public static Map<String, Object> of(Object... keyValues) {
    Map<String, Object> params = new LinkedHashMap<>();
    putAll(params, keyValues);
    return Collections.unmodifiableMap(params);
  }

  public static Map<String, Object> copyOf(Map<String, ?> values) {
    if (values == null || values.isEmpty()) {
      return empty();
    }
    Map<String, Object> params = new LinkedHashMap<>();
    values.forEach(params::put);
    return Collections.unmodifiableMap(params);
  }

  public static Map<String, Object> with(Map<String, ?> values, Object... keyValues) {
    Map<String, Object> params = new LinkedHashMap<>();
    if (values != null) {
      values.forEach(params::put);
    }
    putAll(params, keyValues);
    return Collections.unmodifiableMap(params);
  }

  private static void putAll(Map<String, Object> params, Object... keyValues) {
    if (keyValues.length % 2 != 0) {
      throw new IllegalArgumentException("Template parameters require key/value pairs.");
    }
    for (int i = 0; i < keyValues.length; i += 2) {
      if (!(keyValues[i] instanceof String key)) {
        throw new IllegalArgumentException("Template parameter keys must be strings.");
      }
      params.put(key, keyValues[i + 1]);
    }
  }
}
