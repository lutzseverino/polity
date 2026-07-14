package com.odonta.polity.repository;

import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import java.util.UUID;

public interface ConstitutionalPowerProjection {
  UUID getConstitutionVersionId();

  PowerCode getCode();

  String getName();

  String getNameKey();

  PowerHolderScope getHolderScope();

  String getHolderOfficeCode();
}
