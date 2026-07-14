package com.odonta.polity.mapper;

import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.repository.SanctionProjection;
import com.odonta.polity.result.SanctionResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class)
public interface SanctionApplicationMapper {

  @BeanMapping(ignoreUnmappedSourceProperties = {"motionId", "status"})
  @Mapping(target = "status", source = "resolvedStatus")
  SanctionResult toResult(
      SanctionProjection projection, String targetName, SanctionStatus resolvedStatus);
}
