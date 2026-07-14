package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ConstitutionResponse;
import com.odonta.polity.result.ConstitutionResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    config = PolityMapperConfig.class,
    uses = {
      ConstitutionTransportText.class,
      InstitutionTransportMapper.class,
      ProcedureTransportMapper.class,
      OfficeTransportMapper.class,
      ConstitutionalPowerTransportMapper.class
    })
public interface ConstitutionTransportMapper {
  @Mapping(target = "title", source = ".", qualifiedByName = "constitutionTitle")
  @Mapping(target = "body", source = ".", qualifiedByName = "constitutionBody")
  ConstitutionResponse toResponse(ConstitutionResult result);
}
