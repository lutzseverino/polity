package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionVersionRepository extends JpaRepository<ConstitutionVersion, UUID> {
  Optional<ConstitutionVersion> findEntityById(UUID id);

  Optional<ConstitutionVersion> findEntityByPolityIdAndStatus(
      UUID polityId, ConstitutionStatus status);

  Optional<ConstitutionVersion> findTopEntityByPolityIdOrderByVersionDesc(UUID polityId);
}
