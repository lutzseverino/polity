package com.odonta.polity.repository;

import java.util.UUID;

public interface OfficeProjection {
  UUID getId();

  UUID getJurisdictionId();

  String getCode();

  String getName();

  String getDescription();

  String getNameKey();

  String getDescriptionKey();

  int getTermLengthDays();

  int getSeatCount();
}
