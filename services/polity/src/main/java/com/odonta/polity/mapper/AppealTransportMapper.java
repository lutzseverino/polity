package com.odonta.polity.mapper;

import com.odonta.polity.api.model.AppealResponse;
import com.odonta.polity.result.AppealResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface AppealTransportMapper {
  AppealResponse toResponse(AppealResult result);

  List<AppealResponse> toResponses(List<AppealResult> results);
}
