package com.odonta.polity.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface PolityProjection {
  UUID getId();

  String getName();

  int getConstitutionVersion();

  String getJurisdictionName();

  String getInstitutionName();

  OffsetDateTime getCreatedAt();
}
