package com.odonta.polity.mapper;

import com.odonta.polity.api.model.GovernmentReadinessResponse;
import com.odonta.polity.result.GovernmentReadinessResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = GovernmentAssessmentTransportText.class)
public interface GovernmentReadinessTransportMapper {
  @Mapping(target = "statusMessage", source = ".", qualifiedByName = "readinessStatusMessage")
  @Mapping(target = "diagnostics", source = ".", qualifiedByName = "readinessDiagnostics")
  GovernmentReadinessResponse toResponse(GovernmentReadinessResult result);
}
