package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.PowerCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionalPowerRepository extends JpaRepository<ConstitutionalPower, UUID> {
  Optional<ConstitutionalPower> findByConstitutionVersionIdAndCode(
      UUID constitutionVersionId, PowerCode code);
}
