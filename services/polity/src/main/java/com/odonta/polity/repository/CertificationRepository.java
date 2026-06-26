package com.odonta.polity.repository;

import com.odonta.polity.model.Certification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, UUID> {
  Optional<Certification> findEntityByMotionId(UUID motionId);
}
