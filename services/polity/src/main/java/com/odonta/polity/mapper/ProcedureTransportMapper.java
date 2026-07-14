package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ProcedureResponse;
import com.odonta.polity.result.ProcedureResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = ProcedureTransportText.class)
public interface ProcedureTransportMapper {
  @Mapping(target = "name", source = ".", qualifiedByName = "procedureName")
  ProcedureResponse toResponse(ProcedureResult result);
}
