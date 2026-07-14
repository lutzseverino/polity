package com.odonta.polity.repository;

import com.odonta.polity.model.Institution;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
  Optional<Institution> findEntityByIdAndPolityId(UUID id, UUID polityId);

  List<Institution> findEntitiesByConstitutionVersionId(UUID constitutionVersionId);

  List<InstitutionProjection> findProjectionsByConstitutionVersionId(UUID constitutionVersionId);

  List<InstitutionProjection> findProjectionsByConstitutionVersionIdIn(
      Collection<UUID> constitutionVersionIds);

  List<Institution> findEntitiesByPolityIdAndConstitutionVersionId(
      UUID polityId, UUID constitutionVersionId);
}
