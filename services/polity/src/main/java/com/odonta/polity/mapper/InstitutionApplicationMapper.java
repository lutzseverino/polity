package com.odonta.polity.mapper;

import com.odonta.polity.repository.InstitutionProjection;
import com.odonta.polity.result.InstitutionResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface InstitutionApplicationMapper {
  @BeanMapping(ignoreUnmappedSourceProperties = {"polityId", "constitutionVersionId"})
  InstitutionResult toResult(InstitutionProjection projection);
}
