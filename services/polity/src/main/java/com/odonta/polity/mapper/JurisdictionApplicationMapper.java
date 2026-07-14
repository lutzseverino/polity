package com.odonta.polity.mapper;

import com.odonta.polity.repository.JurisdictionProjection;
import com.odonta.polity.result.JurisdictionResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface JurisdictionApplicationMapper {
  @BeanMapping(ignoreUnmappedSourceProperties = "polityId")
  JurisdictionResult toResult(JurisdictionProjection projection);
}
