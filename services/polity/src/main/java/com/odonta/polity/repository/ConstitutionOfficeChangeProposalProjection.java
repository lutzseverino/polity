package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import java.util.UUID;

public interface ConstitutionOfficeChangeProposalProjection {
  ConstitutionOfficeChangeAction getAction();

  String getOfficeCode();

  UUID getJurisdictionId();

  String getName();

  String getDescription();

  Integer getTermLengthDays();

  Integer getSeatCount();
}
