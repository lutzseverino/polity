package com.odonta.polity.repository;

import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface PolityProjection {
  UUID getId();

  String getName();

  String getSlug();

  PolityVisibility getVisibility();

  PolityStatus getStatus();

  boolean isBootstrapComplete();

  OffsetDateTime getBootstrapCompletedAt();

  OffsetDateTime getCreatedAt();
}
