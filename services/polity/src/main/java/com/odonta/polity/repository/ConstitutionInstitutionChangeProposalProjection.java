package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionChangeOperation;
import com.odonta.polity.model.InstitutionKind;
import java.util.UUID;

public interface ConstitutionInstitutionChangeProposalProjection {
  UUID getAmendmentProposalId();

  ConstitutionChangeOperation getAction();

  UUID getInstitutionId();

  UUID getJurisdictionId();

  String getName();

  InstitutionKind getKind();
}
