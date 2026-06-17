package com.odonta.polity.repository;

import java.util.UUID;

public interface OfficeProjection {
  UUID getId();

  String getCode();

  String getName();

  String getDescription();

  int getTermLengthDays();
}
