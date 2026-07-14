package com.odonta.polity.result;

import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.TemplateParameters;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConstitutionResult(
    UUID id,
    int version,
    String title,
    String body,
    String titleKey,
    String bodyKey,
    Map<String, Object> templateParams,
    ConstitutionStatus status,
    OffsetDateTime ratifiedAt,
    List<InstitutionResult> institutions,
    List<ProcedureResult> procedures,
    List<OfficeResult> offices,
    List<ConstitutionalPowerResult> powers) {
  public ConstitutionResult {
    templateParams = TemplateParameters.copyOf(templateParams);
    institutions = List.copyOf(institutions);
    procedures = List.copyOf(procedures);
    offices = List.copyOf(offices);
    powers = List.copyOf(powers);
  }
}
