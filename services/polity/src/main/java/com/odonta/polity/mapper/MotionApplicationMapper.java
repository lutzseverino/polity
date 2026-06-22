package com.odonta.polity.mapper;

import com.odonta.polity.model.Certification;
import com.odonta.polity.model.CertificationResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface MotionApplicationMapper {

  @BeanMapping(
      ignoreUnmappedSourceProperties = {
        "createdAt",
        "updatedAt",
        "id",
        "polityId",
        "motionId",
        "requestedBy",
        "modality",
        "eligibleCount",
        "yesCount",
        "noCount",
        "abstainCount",
        "electionParticipationCount",
        "electionDecisive",
        "electionWinnerMembershipId",
        "electionWinnerName",
        "quorumRequired",
        "quorumMet",
        "thresholdMet"
      })
  CertificationResult toResult(Certification certification);
}
