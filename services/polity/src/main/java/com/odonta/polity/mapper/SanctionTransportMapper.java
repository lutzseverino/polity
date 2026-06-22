package com.odonta.polity.mapper;

import com.odonta.polity.api.model.SanctionResponse;
import com.odonta.polity.model.SanctionResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface SanctionTransportMapper {
  SanctionResponse toResponse(SanctionResult result);

  List<SanctionResponse> toResponses(List<SanctionResult> results);
}
