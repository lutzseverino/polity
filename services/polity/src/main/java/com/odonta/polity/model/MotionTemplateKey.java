package com.odonta.polity.model;

import java.util.Map;

public enum MotionTemplateKey {
  APPEAL("motion.appeal"),
  CONSTITUTION_AMENDMENT("motion.constitution_amendment"),
  CONSTITUTIONAL_REVIEW("motion.constitutional_review"),
  DISBANDMENT("motion.disbandment"),
  OFFICE_TERM_REVIEW("motion.office_term_review"),
  OFFICE_ELECTION("motion.office_election"),
  SANCTION("motion.sanction");

  private final String keyPrefix;

  MotionTemplateKey(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public String titleKey() {
    return keyPrefix + ".title";
  }

  public String bodyKey() {
    return keyPrefix + ".body";
  }

  public String storedTitle(Map<String, ?> params) {
    return titleKey();
  }

  public String storedBody(Map<String, ?> params) {
    return bodyKey();
  }
}
