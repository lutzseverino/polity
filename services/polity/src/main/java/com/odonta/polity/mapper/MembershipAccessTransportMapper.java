package com.odonta.polity.mapper;

import com.odonta.polity.api.model.MembershipAccessConvergenceResponse;
import com.odonta.polity.result.MembershipAccessConvergenceResult;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface MembershipAccessTransportMapper {
  MembershipAccessConvergenceResponse toResponse(MembershipAccessConvergenceResult result);
}
