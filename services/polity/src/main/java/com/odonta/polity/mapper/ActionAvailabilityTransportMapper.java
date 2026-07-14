package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ActionAvailabilityResponse;
import com.odonta.polity.result.ActionAvailabilityResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = ActionAvailabilityTransportText.class)
public interface ActionAvailabilityTransportMapper {
  @Mapping(target = "reasonMessage", source = ".", qualifiedByName = "availabilityReasonMessage")
  ActionAvailabilityResponse toResponse(ActionAvailabilityResult result);
}
