package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.result.OfficeTermResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class)
public interface OfficeTermApplicationMapper {
  @BeanMapping(ignoreUnmappedSourceProperties = {"officeCode", "status"})
  @Mapping(target = "status", source = "effectiveStatus")
  OfficeTermResult toResult(
      OfficeTermProjection projection,
      String officeName,
      String officeNameKey,
      String memberName,
      OfficeTermStatus effectiveStatus);
}
