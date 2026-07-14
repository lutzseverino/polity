package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeElectionCandidateStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface OfficeElectionCandidateProjection {
  UUID getMotionId();

  UUID getMembershipId();

  OfficeElectionCandidateStatus getStatus();

  OffsetDateTime getRespondedAt();
}
