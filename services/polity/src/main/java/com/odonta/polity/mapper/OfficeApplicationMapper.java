package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficeResult;
import com.odonta.polity.model.OfficeTermResult;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeTermProjection;
import java.util.List;
import java.util.UUID;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class)
public interface OfficeApplicationMapper {
  OfficeResult toResult(OfficeProjection projection);

  List<OfficeResult> toResults(List<OfficeProjection> projections);

  @BeanMapping(ignoreUnmappedSourceProperties = {"officeId", "officeCode", "status"})
  @Mapping(target = "officeId", source = "resolvedOfficeId")
  @Mapping(target = "status", source = "resolvedStatus")
  OfficeTermResult toResult(
      OfficeTermProjection projection,
      UUID resolvedOfficeId,
      String officeName,
      String officeNameKey,
      String memberName,
      OfficeTermStatus resolvedStatus);
}
