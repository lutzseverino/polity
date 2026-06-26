package com.odonta.polity.repository;

import com.odonta.polity.model.Procedure;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedureRepository extends JpaRepository<Procedure, UUID> {
  Optional<Procedure> findEntityById(UUID id);

  Optional<Procedure> findEntityByConstitutionVersionIdAndCode(
      UUID constitutionVersionId, String code);

  List<Procedure> findEntitiesByConstitutionVersionId(UUID constitutionVersionId);

  List<ConstitutionProcedureProjection> findProjectionsByConstitutionVersionId(
      UUID constitutionVersionId);
}
