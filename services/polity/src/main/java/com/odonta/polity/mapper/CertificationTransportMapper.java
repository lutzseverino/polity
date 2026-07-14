package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CertificationResponse;
import com.odonta.polity.result.CertificationResult;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface CertificationTransportMapper {
  CertificationResponse toResponse(CertificationResult result);
}
