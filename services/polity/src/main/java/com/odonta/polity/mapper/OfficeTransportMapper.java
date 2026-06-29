package com.odonta.polity.mapper;

import com.odonta.polity.api.model.OfficeResponse;
import com.odonta.polity.api.model.OfficeTermResponse;
import com.odonta.polity.model.OfficeResult;
import com.odonta.polity.model.OfficeTermResult;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = OfficeTransportConversions.class)
public interface OfficeTransportMapper {
  @Mapping(target = "name", source = ".", qualifiedByName = "officeName")
  @Mapping(target = "description", source = ".", qualifiedByName = "officeDescription")
  OfficeResponse toResponse(OfficeResult result);

  List<OfficeResponse> toResponses(List<OfficeResult> results);

  @Mapping(target = "officeName", source = ".", qualifiedByName = "officeTermOfficeName")
  OfficeTermResponse toResponse(OfficeTermResult result);

  List<OfficeTermResponse> toTermResponses(List<OfficeTermResult> results);
}
