package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CreatePolityRequest;
import com.odonta.polity.api.model.MemberResponse;
import com.odonta.polity.api.model.PolityResponse;
import com.odonta.polity.model.CreatePolityInput;
import com.odonta.polity.model.MembershipResult;
import com.odonta.polity.model.PolityResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface PolityTransportMapper {

  CreatePolityInput toInput(CreatePolityRequest request);

  PolityResponse toResponse(PolityResult result);

  List<PolityResponse> toResponses(List<PolityResult> results);

  MemberResponse toResponse(MembershipResult result);

  List<MemberResponse> toMemberResponses(List<MembershipResult> results);
}
