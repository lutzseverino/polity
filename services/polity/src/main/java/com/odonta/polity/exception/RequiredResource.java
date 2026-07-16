package com.odonta.polity.exception;

import java.util.Map;

public final class RequiredResource {
  private RequiredResource() {}

  public static <K, V> V required(Map<K, V> values, K key, PolityResource resource) {
    V value = values.get(key);
    if (value == null) {
      throw resource.notFound();
    }
    return value;
  }
}
