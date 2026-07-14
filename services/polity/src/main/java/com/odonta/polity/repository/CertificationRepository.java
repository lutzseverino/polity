package com.odonta.polity.repository;

import com.odonta.polity.model.Certification;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, UUID> {
  List<CertificationProjection> findProjectionsByPolityIdAndMotionIdIn(
      UUID polityId, Collection<UUID> motionIds);
}
