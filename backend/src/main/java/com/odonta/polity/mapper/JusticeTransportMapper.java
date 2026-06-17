package com.odonta.polity.mapper;

import com.odonta.polity.api.model.AppealResponse;
import com.odonta.polity.api.model.SanctionResponse;
import com.odonta.polity.model.AppealResult;
import com.odonta.polity.model.SanctionResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface JusticeTransportMapper {
  SanctionResponse toResponse(SanctionResult result);

  List<SanctionResponse> toSanctionResponses(List<SanctionResult> results);

  AppealResponse toResponse(AppealResult result);

  List<AppealResponse> toAppealResponses(List<AppealResult> results);
}
