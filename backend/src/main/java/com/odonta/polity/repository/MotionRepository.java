package com.odonta.polity.repository;

import com.odonta.polity.model.Motion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotionRepository extends JpaRepository<Motion, UUID> {
  Optional<Motion> findByIdAndPolityId(UUID id, UUID polityId);

  List<Motion> findByPolityIdOrderByOpenedAtDesc(UUID polityId);
}
