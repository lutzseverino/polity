package com.odonta.polity.repository;

import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JurisdictionRepository extends JpaRepository<Jurisdiction, UUID> {
  Optional<Jurisdiction> findEntityByPolityIdAndKind(UUID polityId, JurisdictionKind kind);

  List<Jurisdiction> findEntitiesByPolityId(UUID polityId);

  List<JurisdictionProjection> findProjectionsByPolityId(UUID polityId);

  List<JurisdictionProjection> findProjectionsByPolityIdInAndKind(
      Collection<UUID> polityIds, JurisdictionKind kind);
}
