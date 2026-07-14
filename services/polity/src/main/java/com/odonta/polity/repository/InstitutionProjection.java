package com.odonta.polity.repository;

import com.odonta.polity.model.InstitutionKind;
import java.util.UUID;

public interface InstitutionProjection {
  UUID getId();

  UUID getPolityId();

  UUID getConstitutionVersionId();

  UUID getJurisdictionId();

  String getName();

  String getNameKey();

  InstitutionKind getKind();
}
