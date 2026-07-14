package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ConstitutionalHealthResponse;
import com.odonta.polity.result.ConstitutionalHealthResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = GovernmentAssessmentTransportText.class)
public interface ConstitutionalHealthTransportMapper {
  @Mapping(
      target = "statusMessage",
      source = ".",
      qualifiedByName = "constitutionalHealthStatusMessage")
  @Mapping(
      target = "diagnostics",
      source = ".",
      qualifiedByName = "constitutionalHealthDiagnostics")
  ConstitutionalHealthResponse toResponse(ConstitutionalHealthResult result);
}
