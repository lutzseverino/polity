package com.odonta.polity.repository;

import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;

public interface ConstitutionPowerChangeProposalProjection {
  PowerCode getPowerCode();

  PowerHolderScope getHolderScope();

  String getHolderOfficeCode();
}
