package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeTermReviewStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface OfficeTermReviewProjection {
  UUID getId();

  UUID getOfficeTermId();

  UUID getPetitionerMembershipId();

  UUID getVacatedMembershipId();

  String getOfficeName();

  String getOfficeNameKey();

  OfficeTermReviewStatus getStatus();

  String getReason();

  OffsetDateTime getDecidedAt();
}
