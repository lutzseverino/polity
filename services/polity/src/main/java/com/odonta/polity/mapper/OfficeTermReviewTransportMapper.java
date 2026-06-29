package com.odonta.polity.mapper;

import com.odonta.polity.api.model.OfficeTermReviewResponse;
import com.odonta.polity.model.OfficeTermReviewResult;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = OfficeTermReviewTransportConversions.class)
public interface OfficeTermReviewTransportMapper {
  @Mapping(target = "officeName", source = ".", qualifiedByName = "officeTermReviewOfficeName")
  OfficeTermReviewResponse toResponse(OfficeTermReviewResult result);

  List<OfficeTermReviewResponse> toResponses(List<OfficeTermReviewResult> results);
}
