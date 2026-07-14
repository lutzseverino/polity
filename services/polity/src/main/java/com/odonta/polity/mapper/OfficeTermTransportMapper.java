package com.odonta.polity.mapper;

import com.odonta.polity.api.model.OfficeTermResponse;
import com.odonta.polity.result.OfficeTermResult;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = OfficeTermTransportText.class)
public interface OfficeTermTransportMapper {
  @Mapping(target = "officeName", source = ".", qualifiedByName = "officeTermOfficeName")
  OfficeTermResponse toResponse(OfficeTermResult result);

  List<OfficeTermResponse> toResponses(List<OfficeTermResult> results);
}
