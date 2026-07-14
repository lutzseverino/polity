package com.odonta.polity.repository;

import java.util.UUID;

public interface MotionElectorProjection {
  UUID getMotionId();

  UUID getMembershipId();
}
