package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.PowerCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionalPowerRepository extends JpaRepository<ConstitutionalPower, UUID> {
  Optional<ConstitutionalPower> findEntityByConstitutionVersionIdAndCode(
      UUID constitutionVersionId, PowerCode code);

  List<ConstitutionalPower> findEntitiesByConstitutionVersionId(UUID constitutionVersionId);

  List<ConstitutionPowerProjection> findProjectionsByConstitutionVersionId(
      UUID constitutionVersionId);
}
