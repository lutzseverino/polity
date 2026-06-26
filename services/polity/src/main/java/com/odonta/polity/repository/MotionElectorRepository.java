package com.odonta.polity.repository;

import com.odonta.polity.model.MotionElector;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotionElectorRepository extends JpaRepository<MotionElector, UUID> {
  boolean existsByMotionIdAndMembershipId(UUID motionId, UUID membershipId);

  long countByMotionId(UUID motionId);

  List<MotionElector> findEntitiesByMotionId(UUID motionId);
}
