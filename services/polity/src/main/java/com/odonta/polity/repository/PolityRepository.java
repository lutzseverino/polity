package com.odonta.polity.repository;

import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PolityRepository extends JpaRepository<Polity, UUID> {
  long FOUNDER_PRIVATE_POLITY_QUOTA_LOCK_NAMESPACE = 825701L;

  Optional<Polity> findEntityById(UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      value =
          """
      select polity
      from Polity polity
      where polity.id = :id
      """)
  Optional<Polity> findEntityByIdForUpdate(UUID id);

  @Query(
      value =
          """
      select
        p.id as id,
        p.name as name,
        p.slug as slug,
        p.visibility as visibility,
        p.status as status,
        p.createdAt as createdAt
      from Polity p
      left join Membership m on m.polityId = p.id and m.userId = :userId and m.status = :membershipStatus
      where (p.visibility = :publicVisibility or m.id is not null)
        and locate(lower(coalesce(:query, '')), lower(p.name)) > 0
      order by p.createdAt desc, p.id asc
      """,
      countQuery =
          """
      select count(distinct p.id)
      from Polity p
      left join Membership m on m.polityId = p.id and m.userId = :userId and m.status = :membershipStatus
      where (p.visibility = :publicVisibility or m.id is not null)
        and locate(lower(coalesce(:query, '')), lower(p.name)) > 0
      """)
  Page<PolityProjection> findAccessibleProjections(
      UUID userId,
      MembershipStatus membershipStatus,
      PolityVisibility publicVisibility,
      String query,
      Pageable pageable);

  Optional<PolityProjection> findProjectedById(UUID id);

  Optional<PolityProjection> findProjectedBySlug(String slug);

  boolean existsBySlug(String slug);

  boolean existsByIdAndVisibility(UUID id, PolityVisibility visibility);

  @Query(
      value =
          "select 1 from pg_advisory_xact_lock(hashtextextended(cast(:founderId as text), "
              + FOUNDER_PRIVATE_POLITY_QUOTA_LOCK_NAMESPACE
              + "))",
      nativeQuery = true)
  int lockFounderPrivatePolityQuota(UUID founderId);

  @Query(value = "select 1 from pg_advisory_xact_lock(825702)", nativeQuery = true)
  int lockPolitySlugClaims();

  long countByFounderIdAndVisibilityAndStatus(
      UUID founderId, PolityVisibility visibility, PolityStatus status);
}
