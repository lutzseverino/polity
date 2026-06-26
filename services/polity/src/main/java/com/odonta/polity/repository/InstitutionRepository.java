package com.odonta.polity.repository;

import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
  Optional<Institution> findEntityByIdAndPolityId(UUID id, UUID polityId);

  Optional<Institution> findEntityByPolityIdAndConstitutionVersionIdAndKind(
      UUID polityId, UUID constitutionVersionId, InstitutionKind kind);

  List<Institution> findEntitiesByConstitutionVersionId(UUID constitutionVersionId);

  List<ConstitutionInstitutionProjection> findProjectionsByConstitutionVersionId(
      UUID constitutionVersionId);

  List<Institution> findEntitiesByPolityIdAndConstitutionVersionId(
      UUID polityId, UUID constitutionVersionId);
}
