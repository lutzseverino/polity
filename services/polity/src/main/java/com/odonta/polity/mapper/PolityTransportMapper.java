package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ActionAvailabilityResponse;
import com.odonta.polity.api.model.ConstitutionResponse;
import com.odonta.polity.api.model.ConstitutionalHealthResponse;
import com.odonta.polity.api.model.CreatePolityRequest;
import com.odonta.polity.api.model.GovernmentReadinessResponse;
import com.odonta.polity.api.model.PolityActionAvailabilityResponse;
import com.odonta.polity.api.model.PolityResponse;
import com.odonta.polity.model.ActionAvailabilityResult;
import com.odonta.polity.model.ConstitutionInstitutionResult;
import com.odonta.polity.model.ConstitutionPowerResult;
import com.odonta.polity.model.ConstitutionProcedureResult;
import com.odonta.polity.model.ConstitutionResult;
import com.odonta.polity.model.ConstitutionalHealthResult;
import com.odonta.polity.model.CreatePolityInput;
import com.odonta.polity.model.GovernmentReadinessResult;
import com.odonta.polity.model.OfficeResult;
import com.odonta.polity.model.PolityActionAvailabilityResult;
import com.odonta.polity.model.PolityResult;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    config = PolityMapperConfig.class,
    uses = {PolityTransportConversions.class, OfficeTransportConversions.class})
public interface PolityTransportMapper {

  CreatePolityInput toInput(CreatePolityRequest request);

  @Mapping(target = "institutionName", source = ".", qualifiedByName = "summaryInstitutionName")
  PolityResponse toResponse(PolityResult result);

  ConstitutionResponse toResponse(ConstitutionResult result);

  @Mapping(target = "name", source = ".", qualifiedByName = "institutionName")
  com.odonta.polity.api.model.ConstitutionInstitutionResponse toResponse(
      ConstitutionInstitutionResult result);

  @Mapping(target = "name", source = ".", qualifiedByName = "procedureName")
  com.odonta.polity.api.model.ConstitutionProcedureResponse toResponse(
      ConstitutionProcedureResult result);

  @Mapping(target = "name", source = ".", qualifiedByName = "officeName")
  @Mapping(target = "description", source = ".", qualifiedByName = "officeDescription")
  com.odonta.polity.api.model.OfficeResponse toResponse(OfficeResult result);

  @Mapping(target = "name", source = ".", qualifiedByName = "powerName")
  com.odonta.polity.api.model.ConstitutionPowerResponse toResponse(ConstitutionPowerResult result);

  @Mapping(target = "reasonMessage", source = ".", qualifiedByName = "availabilityReasonMessage")
  ActionAvailabilityResponse toResponse(ActionAvailabilityResult result);

  @Mapping(target = "statusMessage", source = ".", qualifiedByName = "readinessStatusMessage")
  @Mapping(target = "diagnostics", source = ".", qualifiedByName = "readinessDiagnostics")
  GovernmentReadinessResponse toResponse(GovernmentReadinessResult result);

  @Mapping(
      target = "statusMessage",
      source = ".",
      qualifiedByName = "constitutionalHealthStatusMessage")
  @Mapping(
      target = "diagnostics",
      source = ".",
      qualifiedByName = "constitutionalHealthDiagnostics")
  ConstitutionalHealthResponse toResponse(ConstitutionalHealthResult result);

  PolityActionAvailabilityResponse toResponse(PolityActionAvailabilityResult result);

  List<PolityResponse> toResponses(List<PolityResult> results);
}
