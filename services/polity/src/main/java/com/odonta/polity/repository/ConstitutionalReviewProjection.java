package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionalReviewStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface ConstitutionalReviewProjection {
  UUID getId();

  UUID getTargetRecordId();

  UUID getPetitionerMembershipId();

  ConstitutionalReviewStatus getStatus();

  String getReason();

  OffsetDateTime getDecidedAt();
}
