package com.odonta.polity.repository;

import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface SanctionProjection {
  UUID getId();

  UUID getTargetMembershipId();

  String getTargetName();

  SanctionType getType();

  SanctionStatus getStatus();

  String getReason();

  OffsetDateTime getStartedAt();

  OffsetDateTime getEndsAt();
}
