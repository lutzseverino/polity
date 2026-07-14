package com.odonta.polity.mapper;

import com.odonta.polity.api.model.GovernmentStructureResponse;
import com.odonta.polity.result.GovernmentStructureResult;
import org.mapstruct.Mapper;

@Mapper(
    config = PolityMapperConfig.class,
    uses = {
      ConstitutionTransportMapper.class,
      JurisdictionTransportMapper.class,
      GovernmentFormationTransportMapper.class
    })
public interface GovernmentStructureTransportMapper {
  GovernmentStructureResponse toResponse(GovernmentStructureResult result);
}
