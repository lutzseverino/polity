package com.odonta.polity.repository;

import java.util.UUID;

public interface OfficeElectionBallotPreferenceProjection {
  UUID getMotionId();

  UUID getMembershipId();

  UUID getCandidateMembershipId();

  int getRank();
}
