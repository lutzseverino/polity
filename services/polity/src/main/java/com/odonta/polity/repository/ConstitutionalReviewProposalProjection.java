package com.odonta.polity.repository;

import java.util.UUID;

public interface ConstitutionalReviewProposalProjection {
  UUID getMotionId();

  UUID getTargetRecordId();

  UUID getPetitionerMembershipId();

  String getReason();
}
