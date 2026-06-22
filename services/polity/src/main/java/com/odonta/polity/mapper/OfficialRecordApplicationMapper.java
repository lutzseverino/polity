package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficialRecordResult;
import com.odonta.polity.repository.OfficialRecordProjection;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class)
public interface OfficialRecordApplicationMapper {

  @BeanMapping(ignoreUnmappedSourceProperties = {"constitutionVersionId", "actorMembershipId"})
  @Mapping(target = "actorName", source = "actorName")
  @Mapping(target = "constitutionVersion", source = "constitutionVersion")
  OfficialRecordResult toResult(
      OfficialRecordProjection projection, String actorName, int constitutionVersion);
}
