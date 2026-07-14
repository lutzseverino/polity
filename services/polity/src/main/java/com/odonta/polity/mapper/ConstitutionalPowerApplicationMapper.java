package com.odonta.polity.mapper;

import com.odonta.polity.repository.ConstitutionalPowerProjection;
import com.odonta.polity.result.ConstitutionalPowerResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface ConstitutionalPowerApplicationMapper {
  @BeanMapping(ignoreUnmappedSourceProperties = "constitutionVersionId")
  ConstitutionalPowerResult toResult(ConstitutionalPowerProjection projection);
}
