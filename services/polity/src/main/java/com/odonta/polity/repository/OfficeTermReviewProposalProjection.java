package com.odonta.polity.repository;

import java.util.UUID;

public interface OfficeTermReviewProposalProjection {
  UUID getMotionId();

  UUID getOfficeTermId();

  UUID getPetitionerMembershipId();

  String getReason();
}
