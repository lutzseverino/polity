package com.odonta.polity.repository;

import com.odonta.polity.model.Appeal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppealRepository extends JpaRepository<Appeal, UUID> {
  boolean existsByPolityIdAndSanctionId(UUID polityId, UUID sanctionId);

  Page<AppealProjection> findProjectionsByPolityIdOrderByDecidedAtDescIdAsc(
      UUID polityId, Pageable pageable);
}
