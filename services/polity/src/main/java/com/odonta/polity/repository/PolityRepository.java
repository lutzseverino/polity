package com.odonta.polity.repository;

import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityVisibility;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PolityRepository extends JpaRepository<Polity, UUID> {

  Optional<Polity> findEntityById(UUID id);

  @Query(
      """
      select
        p.id as id,
        p.name as name,
        p.visibility as visibility,
        p.status as status,
        p.createdAt as createdAt
      from Polity p
      left join Membership m on m.polityId = p.id and m.userId = :userId and m.status = :membershipStatus
      where p.visibility = :publicVisibility or m.id is not null
      order by p.createdAt desc
      """)
  List<PolityProjection> findAccessibleProjections(
      UUID userId, MembershipStatus membershipStatus, PolityVisibility publicVisibility);

  Optional<PolityProjection> findProjectedById(UUID id);

  boolean existsByIdAndVisibility(UUID id, PolityVisibility visibility);
}
