package com.odonta.polity.repository;

import com.odonta.polity.model.Office;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeRepository extends JpaRepository<Office, UUID> {
  List<Office> findByConstitutionVersionIdOrderByName(UUID constitutionVersionId);

  List<OfficeProjection> findProjectionsByConstitutionVersionIdOrderByName(
      UUID constitutionVersionId);

  Optional<OfficeProjection> findProjectedById(UUID id);

  Optional<Office> findByIdAndPolityId(UUID id, UUID polityId);

  Optional<Office> findByConstitutionVersionIdAndCode(UUID constitutionVersionId, String code);
}
