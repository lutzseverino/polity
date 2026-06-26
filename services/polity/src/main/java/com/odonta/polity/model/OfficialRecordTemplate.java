package com.odonta.polity.model;

import java.util.Map;

public final class OfficialRecordTemplate {
  private final String titleKey;
  private final String bodyKey;
  private final String storedTitle;
  private final String storedBody;
  private final Map<String, Object> params;

  private OfficialRecordTemplate(OfficialRecordTemplateKey key, Map<String, ?> params) {
    Map<String, ?> safeParams = params == null ? Map.of() : params;
    this.titleKey = key.titleKey();
    this.bodyKey = key.bodyKey();
    this.storedTitle = key.storedTitle(safeParams);
    this.storedBody = key.storedBody(safeParams);
    this.params = TemplateParameters.copyOf(safeParams);
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

  public String storedTitle() {
    return storedTitle;
  }

  public String storedBody() {
    return storedBody;
  }

  public Map<String, Object> params() {
    return params;
  }
}
