package com.odonta.polity.repository;

import com.odonta.polity.model.JurisdictionKind;
import java.util.UUID;

public interface ConstitutionJurisdictionProjection {
  UUID getId();

  String getName();

  JurisdictionKind getKind();
}
