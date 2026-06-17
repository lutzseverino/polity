package com.odonta.polity.repository;

import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SanctionRepository extends JpaRepository<Sanction, UUID> {
  Optional<Sanction> findByIdAndPolityId(UUID id, UUID polityId);

  boolean existsByPolityIdAndTargetMembershipIdAndTypeAndStatusAndEndsAtAfter(
      UUID polityId,
      UUID targetMembershipId,
      SanctionType type,
      SanctionStatus status,
      OffsetDateTime now);

  @Query(
      """
      select
        sanction.id as id,
        target.id as targetMembershipId,
        target.displayName as targetName,
        sanction.type as type,
        sanction.status as status,
        sanction.reason as reason,
        sanction.startedAt as startedAt,
        sanction.endsAt as endsAt
      from Sanction sanction
      join Membership target on target.id = sanction.targetMembershipId
      where sanction.polityId = :polityId
      order by sanction.startedAt desc
      """)
  List<SanctionProjection> findProjectionsByPolityId(UUID polityId);
}
