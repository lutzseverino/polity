package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionInstitutionChangeAction;
import com.odonta.polity.model.InstitutionKind;
import java.util.UUID;

public interface ConstitutionInstitutionChangeProposalProjection {
  UUID getAmendmentProposalId();

  ConstitutionInstitutionChangeAction getAction();

  UUID getInstitutionId();

  UUID getJurisdictionId();

  String getName();

  InstitutionKind getKind();
}
