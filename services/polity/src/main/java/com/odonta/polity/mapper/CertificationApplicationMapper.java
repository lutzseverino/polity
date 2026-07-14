package com.odonta.polity.mapper;

import com.odonta.polity.repository.CertificationProjection;
import com.odonta.polity.result.CertificationResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface CertificationApplicationMapper {

  @BeanMapping(ignoreUnmappedSourceProperties = {"motionId", "electionTallySnapshot"})
  CertificationResult toResult(CertificationProjection certification);
}
