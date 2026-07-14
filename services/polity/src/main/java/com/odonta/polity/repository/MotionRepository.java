package com.odonta.polity.repository;

import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotionRepository extends JpaRepository<Motion, UUID> {
  Optional<Motion> findEntityByIdAndPolityId(UUID id, UUID polityId);

  boolean existsByIdAndStatus(UUID id, MotionStatus status);

  Page<MotionProjection> findProjectionsByPolityIdOrderByOpenedAtDescIdAsc(
      UUID polityId, Pageable pageable);

  Optional<MotionProjection> findProjectedByIdAndPolityId(UUID id, UUID polityId);
}
