package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ConstitutionResponse;
import com.odonta.polity.api.model.CreatePolityRequest;
import com.odonta.polity.api.model.PolityActionAvailabilityResponse;
import com.odonta.polity.api.model.PolityResponse;
import com.odonta.polity.model.ConstitutionResult;
import com.odonta.polity.model.CreatePolityInput;
import com.odonta.polity.model.PolityActionAvailabilityResult;
import com.odonta.polity.model.PolityResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface PolityTransportMapper {

  CreatePolityInput toInput(CreatePolityRequest request);

  PolityResponse toResponse(PolityResult result);

  ConstitutionResponse toResponse(ConstitutionResult result);

  PolityActionAvailabilityResponse toResponse(PolityActionAvailabilityResult result);

  List<PolityResponse> toResponses(List<PolityResult> results);
}
