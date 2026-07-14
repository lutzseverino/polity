package com.odonta.polity.repository;

import com.odonta.polity.model.VoteChoice;
import java.util.UUID;

public interface VoteProjection {
  UUID getMotionId();

  UUID getMembershipId();

  VoteChoice getChoice();
}
