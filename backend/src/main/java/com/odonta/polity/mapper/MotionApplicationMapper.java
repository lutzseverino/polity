package com.odonta.polity.mapper;

import com.odonta.polity.model.Certification;
import com.odonta.polity.model.CertificationResult;
import com.odonta.polity.model.MotionResult;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.repository.MotionProjection;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class)
public interface MotionApplicationMapper {

  @Mapping(target = "id", source = "motion.id")
  @Mapping(target = "tally", source = "tally")
  @Mapping(target = "certification", source = "certification")
  @BeanMapping(ignoreUnmappedSourceProperties = {"quorumNumerator", "quorumDenominator"})
  MotionResult toResult(MotionProjection motion, VotingResult tally, Certification certification);

  @BeanMapping(
      ignoreUnmappedSourceProperties = {
        "createdAt",
        "updatedAt",
        "id",
        "polityId",
        "motionId",
        "requestedBy",
        "eligibleCount",
        "yesCount",
        "noCount",
        "abstainCount",
        "quorumRequired",
        "quorumMet",
        "thresholdMet"
      })
  CertificationResult toResult(Certification certification);
}
