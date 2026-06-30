package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeElectionMethod;
import java.util.UUID;

public interface OfficeElectionProposalProjection {
  UUID getPolityId();

  UUID getOfficeId();

  int getSeatsAvailable();

  OfficeElectionMethod getMethod();
}
