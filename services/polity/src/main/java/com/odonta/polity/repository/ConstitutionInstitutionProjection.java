package com.odonta.polity.repository;

import com.odonta.polity.model.InstitutionKind;
import java.util.UUID;

public interface ConstitutionInstitutionProjection {
  UUID getId();

  UUID getJurisdictionId();

  String getName();

  String getNameKey();

  InstitutionKind getKind();
}
