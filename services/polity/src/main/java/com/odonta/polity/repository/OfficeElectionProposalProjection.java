package com.odonta.polity.repository;

import java.util.UUID;

public interface OfficeElectionProposalProjection {
  UUID getPolityId();

  UUID getOfficeId();
}
