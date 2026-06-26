package com.odonta.polity.repository;

import java.util.UUID;

public interface AppealProposalProjection {
  UUID getMotionId();

  UUID getSanctionId();

  UUID getAppellantMembershipId();

  String getReason();
}
