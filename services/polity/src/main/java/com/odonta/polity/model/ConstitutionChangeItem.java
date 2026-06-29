package com.odonta.polity.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ConstitutionChangeItem(
    ConstitutionChangeKind kind,
    ConstitutionChangeOperation operation,
    String subject,
    String subjectKey,
    Map<String, Object> details) {

  public ConstitutionChangeItem {
    details =
        details == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(details));
  }

  public ConstitutionChangeItem(
      ConstitutionChangeKind kind,
      ConstitutionChangeOperation operation,
      String subject,
      Map<String, Object> details) {
    this(kind, operation, subject, null, details);
  }

  public Map<String, Object> toTemplateParameter() {
    return TemplateParameters.ofPresent(
        "kind",
        kind.value(),
        "operation",
        operation.value(),
        "subject",
        subject,
        "subjectKey",
        subjectKey,
        "details",
        details);
  }
}
