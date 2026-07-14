package com.odonta.polity.mapper;

import com.odonta.polity.api.model.JurisdictionResponse;
import com.odonta.polity.result.JurisdictionResult;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface JurisdictionTransportMapper {
  JurisdictionResponse toResponse(JurisdictionResult result);
}
