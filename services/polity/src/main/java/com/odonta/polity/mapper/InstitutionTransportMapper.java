package com.odonta.polity.mapper;

import com.odonta.polity.api.model.InstitutionResponse;
import com.odonta.polity.result.InstitutionResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = InstitutionTransportText.class)
public interface InstitutionTransportMapper {
  @Mapping(target = "name", source = ".", qualifiedByName = "institutionName")
  InstitutionResponse toResponse(InstitutionResult result);
}
