package com.odonta.polity.repository;

import com.odonta.polity.model.JurisdictionKind;
import java.util.UUID;

public interface JurisdictionProjection {
  UUID getId();

  UUID getPolityId();

  String getName();

  JurisdictionKind getKind();
}
