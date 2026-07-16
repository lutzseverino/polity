package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ActionAvailabilityResponse;
import com.odonta.polity.result.ActionAvailabilityResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = ActionAvailabilityTransportText.class)
public interface ActionAvailabilityTransportMapper {
  @Mapping(target = "available", expression = "java(result.available())")
  @Mapping(target = "reason", expression = "java(toTransport(result.reason()))")
  @Mapping(target = "reasonMessage", source = ".", qualifiedByName = "availabilityReasonMessage")
  ActionAvailabilityResponse toResponse(ActionAvailabilityResult result);

  default com.odonta.polity.api.model.ActionUnavailableReason toTransport(
      com.odonta.polity.result.ActionUnavailableReason reason) {
    return reason == null
        ? null
        : com.odonta.polity.api.model.ActionUnavailableReason.fromValue(reason.wireValue());
  }
}
