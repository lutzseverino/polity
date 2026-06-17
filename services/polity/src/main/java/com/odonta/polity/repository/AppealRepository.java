package com.odonta.polity.repository;

import com.odonta.polity.model.Appeal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppealRepository extends JpaRepository<Appeal, UUID> {
  boolean existsByPolityIdAndSanctionId(UUID polityId, UUID sanctionId);

  @Query(
      """
      select
        appeal.id as id,
        appeal.sanctionId as sanctionId,
        appellant.id as appellantMembershipId,
        appellant.displayName as appellantName,
        appeal.status as status,
        appeal.reason as reason,
        appeal.decidedAt as decidedAt
      from Appeal appeal
      join Membership appellant on appellant.id = appeal.appellantMembershipId
      where appeal.polityId = :polityId
      order by appeal.decidedAt desc
      """)
  List<AppealProjection> findProjectionsByPolityId(UUID polityId);
}
