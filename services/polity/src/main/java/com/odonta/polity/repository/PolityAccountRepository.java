package com.odonta.polity.repository;

import com.odonta.polity.model.PolityAccount;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PolityAccountRepository extends JpaRepository<PolityAccount, UUID> {
  long PROVISIONING_LOCK_NAMESPACE = 825703L;

  @Query(
      value =
          "select 1 from pg_advisory_xact_lock(hashtextextended(cast(:userId as text), "
              + PROVISIONING_LOCK_NAMESPACE
              + "))",
      nativeQuery = true)
  int lockProvisioning(UUID userId);
}
