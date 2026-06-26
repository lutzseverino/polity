package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionalReviewStatus;
import com.odonta.polity.model.OfficialRecordType;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface ConstitutionalReviewProjection {
  UUID getId();

  UUID getTargetRecordId();

  int getTargetEntryNumber();

  OfficialRecordType getTargetType();

  UUID getPetitionerMembershipId();

  ConstitutionalReviewStatus getStatus();

  String getReason();

  OffsetDateTime getDecidedAt();
}
