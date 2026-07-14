package com.odonta.polity.mapper;

import com.odonta.polity.api.model.GovernmentFormationResponse;
import com.odonta.polity.result.GovernmentFormationResult;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface GovernmentFormationTransportMapper {
  GovernmentFormationResponse toResponse(GovernmentFormationResult result);
}
