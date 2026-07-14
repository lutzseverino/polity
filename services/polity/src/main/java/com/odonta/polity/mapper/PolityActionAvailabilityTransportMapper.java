package com.odonta.polity.mapper;

import com.odonta.polity.api.model.PolityActionAvailabilityResponse;
import com.odonta.polity.result.PolityActionAvailabilityResult;
import org.mapstruct.Mapper;

@Mapper(
    config = PolityMapperConfig.class,
    uses = {
      ActionAvailabilityTransportMapper.class,
      GovernmentReadinessTransportMapper.class,
      ConstitutionalHealthTransportMapper.class
    })
public interface PolityActionAvailabilityTransportMapper {
  PolityActionAvailabilityResponse toResponse(PolityActionAvailabilityResult result);
}
