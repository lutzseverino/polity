package com.odonta.polity.repository;

import com.odonta.polity.model.Office;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeRepository extends JpaRepository<Office, UUID> {
  List<Office> findEntitiesByConstitutionVersionIdOrderByName(UUID constitutionVersionId);

  List<OfficeProjection> findProjectionsByConstitutionVersionIdOrderByName(
      UUID constitutionVersionId);

  Optional<OfficeProjection> findProjectedById(UUID id);

  Optional<Office> findEntityByIdAndPolityId(UUID id, UUID polityId);

  Optional<Office> findEntityByConstitutionVersionIdAndCode(
      UUID constitutionVersionId, String code);
}
