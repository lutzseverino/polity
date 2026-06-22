package com.odonta.polity.model;

import java.util.Map;

public final class OfficialRecordTemplate {
  private final String titleKey;
  private final String bodyKey;
  private final String fallbackTitle;
  private final String fallbackBody;
  private final Map<String, Object> params;

  private OfficialRecordTemplate(OfficialRecordTemplateKey key, Map<String, ?> params) {
    Map<String, ?> safeParams = params == null ? Map.of() : params;
    this.titleKey = key.titleKey();
    this.bodyKey = key.bodyKey();
    this.fallbackTitle = key.fallbackTitle(safeParams);
    this.fallbackBody = key.fallbackBody(safeParams);
    this.params = Map.copyOf(safeParams);
  }

  public static OfficialRecordTemplate of(OfficialRecordTemplateKey key, Map<String, ?> params) {
    return new OfficialRecordTemplate(key, params);
  }

  public String titleKey() {
    return titleKey;
  }

  public String bodyKey() {
    return bodyKey;
  }

  public String fallbackTitle() {
    return fallbackTitle;
  }

  public String fallbackBody() {
    return fallbackBody;
  }

  public Map<String, Object> params() {
    return params;
  }
}
