package com.odonta.polity.mapper;

import com.odonta.polity.repository.ProcedureProjection;
import com.odonta.polity.result.ProcedureResult;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface ProcedureApplicationMapper {
  ProcedureResult toResult(ProcedureProjection projection);
}
