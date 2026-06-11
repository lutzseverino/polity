package com.odonta.polity.repository;

import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JurisdictionRepository extends JpaRepository<Jurisdiction, UUID> {
  Optional<Jurisdiction> findByPolityIdAndKind(UUID polityId, JurisdictionKind kind);
}
