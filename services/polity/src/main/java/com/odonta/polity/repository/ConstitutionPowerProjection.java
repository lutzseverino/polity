package com.odonta.polity.repository;

import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;

public interface ConstitutionPowerProjection {
  PowerCode getCode();

  String getName();

  String getNameKey();

  PowerHolderScope getHolderScope();

  String getHolderOfficeCode();
}
