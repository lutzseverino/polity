package com.odonta.polity.mapper;

import com.odonta.polity.api.model.MemberResponse;
import com.odonta.polity.model.MembershipResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface MembershipTransportMapper {
  MemberResponse toResponse(MembershipResult result);

  List<MemberResponse> toResponses(List<MembershipResult> results);
}
