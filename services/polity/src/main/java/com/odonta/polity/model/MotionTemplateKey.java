package com.odonta.polity.model;

import java.util.Map;

public enum MotionTemplateKey {
  APPEAL("motion.appeal", "Appeal sanction", "{reason}"),
  CONSTITUTION_AMENDMENT("motion.constitution_amendment", "Amend constitution: {title}", "{body}"),
  DISBANDMENT("motion.disbandment", "Disband polity: {title}", "{body}"),
  OFFICE_ELECTION(
      "motion.office_election",
      "Elect {officeName}",
      "Election for {officeName} with candidates: {candidateNames}."),
  SANCTION("motion.sanction", "Sanction {targetName}", "{reason}");

  private final String keyPrefix;
  private final String fallbackTitle;
  private final String fallbackBody;

  MotionTemplateKey(String keyPrefix, String fallbackTitle, String fallbackBody) {
    this.keyPrefix = keyPrefix;
    this.fallbackTitle = fallbackTitle;
    this.fallbackBody = fallbackBody;
  }

  public String titleKey() {
    return keyPrefix + ".title";
  }

  public String bodyKey() {
    return keyPrefix + ".body";
  }

  public String fallbackTitle(Map<String, ?> params) {
    return TemplateText.render(fallbackTitle, params);
  }

  public String fallbackBody(Map<String, ?> params) {
    return TemplateText.render(fallbackBody, params);
  }
}
