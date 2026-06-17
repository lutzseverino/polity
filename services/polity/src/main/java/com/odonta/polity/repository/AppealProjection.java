package com.odonta.polity.repository;

import com.odonta.polity.model.AppealStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface AppealProjection {
  UUID getId();

  UUID getSanctionId();

  UUID getAppellantMembershipId();

  String getAppellantName();

  AppealStatus getStatus();

  String getReason();

  OffsetDateTime getDecidedAt();
}
