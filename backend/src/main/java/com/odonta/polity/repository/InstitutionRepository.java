package com.odonta.polity.repository;

import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
  Optional<Institution> findByPolityIdAndConstitutionVersionIdAndKind(
      UUID polityId, UUID constitutionVersionId, InstitutionKind kind);

  List<Institution> findByConstitutionVersionId(UUID constitutionVersionId);
}
