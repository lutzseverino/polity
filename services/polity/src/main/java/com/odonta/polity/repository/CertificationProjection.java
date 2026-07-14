package com.odonta.polity.repository;

import com.odonta.polity.model.CertificationModality;
import com.odonta.polity.model.CertificationOutcomeReason;
import com.odonta.polity.model.OfficeElectionTallyResult;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface CertificationProjection {
  UUID getMotionId();

  CertificationModality getModality();

  int getEligibleCount();

  Integer getYesCount();

  Integer getNoCount();

  Integer getAbstainCount();

  Integer getElectionParticipationCount();

  Boolean getElectionDecisive();

  Integer getElectionWinnerCount();

  int getQuorumRequired();

  boolean isQuorumMet();

  boolean isThresholdMet();

  boolean isPassed();

  CertificationOutcomeReason getOutcomeReason();

  OffsetDateTime getCertifiedAt();

  OfficeElectionTallyResult getElectionTallySnapshot();
}
