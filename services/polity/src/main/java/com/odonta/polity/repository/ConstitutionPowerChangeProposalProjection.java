package com.odonta.polity.repository;

import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import java.util.UUID;

public interface ConstitutionPowerChangeProposalProjection {
  UUID getAmendmentProposalId();

  PowerCode getPowerCode();

  PowerHolderScope getHolderScope();

  String getHolderOfficeCode();
}
