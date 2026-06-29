package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeTermStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface OfficeTermProjection {
  UUID getId();

  UUID getOfficeId();

  String getOfficeCode();

  UUID getMembershipId();

  OfficeTermStatus getStatus();

  OffsetDateTime getStartedAt();

  OffsetDateTime getEndsAt();
}
