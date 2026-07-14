package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionStatus;
import java.util.UUID;

public interface ConstitutionVersionProjection {
  UUID getId();

  UUID getPolityId();

  int getVersion();

  ConstitutionStatus getStatus();
}
