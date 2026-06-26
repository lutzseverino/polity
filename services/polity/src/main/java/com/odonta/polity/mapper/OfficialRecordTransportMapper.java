package com.odonta.polity.mapper;

import com.odonta.polity.api.model.OfficialRecordEntryResponse;
import com.odonta.polity.model.OfficialRecordResult;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = OfficialRecordResponseConversions.class)
public interface OfficialRecordTransportMapper {

  @Mapping(target = "title", source = ".", qualifiedByName = "officialRecordTitle")
  @Mapping(target = "body", source = ".", qualifiedByName = "officialRecordBody")
  OfficialRecordEntryResponse toResponse(OfficialRecordResult result);

  List<OfficialRecordEntryResponse> toResponses(List<OfficialRecordResult> results);
}
