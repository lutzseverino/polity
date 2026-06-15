package com.odonta.polity.repository;

import com.odonta.polity.model.MembershipStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface MembershipProjection {
  UUID getId();

  UUID getUserId();

  String getName();

  String getEmail();

  MembershipStatus getStatus();

  OffsetDateTime getAdmittedAt();
}
