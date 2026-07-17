package com.odonta.polity.mapper;

import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.result.PolitySummaryResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface PolityApplicationMapper {

  @BeanMapping(ignoreUnmappedSourceProperties = {"bootstrapComplete", "bootstrapCompletedAt"})
  PolitySummaryResult toSummary(
      PolityProjection projection,
      int constitutionVersion,
      String jurisdictionName,
      String institutionName,
      String institutionNameKey);
}
