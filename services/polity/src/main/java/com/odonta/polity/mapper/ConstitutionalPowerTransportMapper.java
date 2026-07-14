package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ConstitutionalPowerResponse;
import com.odonta.polity.result.ConstitutionalPowerResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = ConstitutionalPowerTransportText.class)
public interface ConstitutionalPowerTransportMapper {
  @Mapping(target = "name", source = ".", qualifiedByName = "powerName")
  ConstitutionalPowerResponse toResponse(ConstitutionalPowerResult result);
}
