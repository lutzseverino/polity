package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionVersionRepository extends JpaRepository<ConstitutionVersion, UUID> {
  Optional<ConstitutionVersion> findByPolityIdAndStatus(UUID polityId, ConstitutionStatus status);
}
