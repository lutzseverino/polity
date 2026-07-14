package com.odonta.polity.repository;

import com.odonta.polity.model.Office;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeRepository extends JpaRepository<Office, UUID> {
  List<Office> findEntitiesByConstitutionVersionIdOrderByName(UUID constitutionVersionId);

  Page<OfficeProjection> findProjectionsByConstitutionVersionIdOrderByNameAscIdAsc(
      UUID constitutionVersionId, Pageable pageable);

  List<OfficeProjection> findProjectionsByConstitutionVersionIdOrderByNameAscIdAsc(
      UUID constitutionVersionId);

  Optional<OfficeProjection> findProjectedById(UUID id);

  List<OfficeProjection> findProjectionsByPolityIdAndIdIn(UUID polityId, Collection<UUID> ids);

  Optional<Office> findEntityByIdAndPolityId(UUID id, UUID polityId);

  Optional<Office> findEntityByConstitutionVersionIdAndCode(
      UUID constitutionVersionId, String code);

  boolean existsByConstitutionVersionIdAndCode(UUID constitutionVersionId, String code);
}
