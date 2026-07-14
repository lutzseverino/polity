package com.odonta.polity.repository;

import java.util.UUID;

public interface ConstitutionAmendmentProposalProjection {
  UUID getId();

  UUID getMotionId();

  String getTitle();

  String getBody();
}
