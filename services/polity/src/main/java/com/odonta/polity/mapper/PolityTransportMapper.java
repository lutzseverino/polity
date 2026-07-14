package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CreatePolityRequest;
import com.odonta.polity.api.model.PolityResponse;
import com.odonta.polity.input.CreatePolityInput;
import com.odonta.polity.result.PolitySummaryResult;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = PolityTransportText.class)
public interface PolityTransportMapper {

  CreatePolityInput toInput(CreatePolityRequest request);

  @Mapping(target = "institutionName", source = ".", qualifiedByName = "summaryInstitutionName")
  PolityResponse toResponse(PolitySummaryResult result);

  List<PolityResponse> toResponses(List<PolitySummaryResult> results);
}
