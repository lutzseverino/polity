package com.odonta.polity.mapper;

import com.odonta.polity.api.model.OfficeResponse;
import com.odonta.polity.result.OfficeResult;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = OfficeTransportText.class)
public interface OfficeTransportMapper {
  @Mapping(target = "name", source = ".", qualifiedByName = "officeName")
  @Mapping(target = "description", source = ".", qualifiedByName = "officeDescription")
  OfficeResponse toResponse(OfficeResult result);

  List<OfficeResponse> toResponses(List<OfficeResult> results);
}
