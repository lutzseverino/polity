package com.odonta.polity.repository;

import com.odonta.polity.model.Appeal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppealRepository extends JpaRepository<Appeal, UUID> {
  boolean existsByPolityIdAndSanctionId(UUID polityId, UUID sanctionId);

  List<AppealProjection> findProjectionsByPolityIdOrderByDecidedAtDesc(UUID polityId);
}
