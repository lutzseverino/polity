package com.odonta.polity.mapper;

import com.odonta.polity.model.ActionAvailabilityResult;
import com.odonta.polity.model.ConstitutionInstitutionResult;
import com.odonta.polity.model.ConstitutionPowerResult;
import com.odonta.polity.model.ConstitutionProcedureResult;
import com.odonta.polity.model.PolityResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolityTransportConversions {
  private final TransportTextResolver text;

  @Named("summaryInstitutionName")
  public String summaryInstitutionName(PolityResult result) {
    return text.resolveName(result.institutionNameKey(), result.institutionName());
  }

  @Named("institutionName")
  public String institutionName(ConstitutionInstitutionResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }

  @Named("procedureName")
  public String procedureName(ConstitutionProcedureResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }

  @Named("powerName")
  public String powerName(ConstitutionPowerResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }

  @Named("availabilityReasonMessage")
  public String availabilityReasonMessage(ActionAvailabilityResult result) {
    if (result == null || result.reason() == null || result.reason().isBlank()) {
      return null;
    }
    return text.resolveName("api_error." + result.reason(), result.reason());
  }
}
