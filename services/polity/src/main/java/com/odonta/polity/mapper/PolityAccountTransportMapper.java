package com.odonta.polity.mapper;

import com.odonta.polity.api.model.PolityAccountResponse;
import com.odonta.polity.result.PolityAccountResult;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface PolityAccountTransportMapper {
  PolityAccountResponse toResponse(PolityAccountResult result);
}
