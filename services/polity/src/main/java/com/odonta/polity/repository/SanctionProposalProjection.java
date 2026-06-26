package com.odonta.polity.repository;

import com.odonta.polity.model.SanctionType;
import java.util.UUID;

public interface SanctionProposalProjection {
  UUID getTargetMembershipId();

  SanctionType getType();

  String getReason();

  int getDurationDays();
}
