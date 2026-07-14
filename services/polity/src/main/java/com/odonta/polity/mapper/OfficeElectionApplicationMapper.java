package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.result.OfficeElectionBallotResult;
import com.odonta.polity.result.OfficeElectionCandidateResult;
import com.odonta.polity.result.OfficeElectionResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface OfficeElectionApplicationMapper {
  OfficeElectionCandidateResult toCandidateResult(
      UUID membershipId,
      String name,
      OfficeElectionCandidateStatus status,
      OffsetDateTime respondedAt);

  OfficeElectionResult toResult(
      UUID officeId,
      String officeCode,
      String officeName,
      String officeNameKey,
      int seatsAvailable,
      OfficeElectionMethod method,
      OfficeElectionBallotResult currentBallot,
      List<OfficeElectionCandidateResult> candidates);
}
