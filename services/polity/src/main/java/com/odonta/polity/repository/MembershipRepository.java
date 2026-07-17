package com.odonta.polity.repository;

import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
  Optional<Membership> findEntityById(UUID id);

  Optional<Membership> findEntityByPolityIdAndUserIdAndStatus(
      UUID polityId, UUID userId, MembershipStatus status);

  Optional<Membership> findEntityByPolityIdAndUserId(UUID polityId, UUID userId);

  List<Membership> findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
      UUID polityId, MembershipStatus status);

  Page<MembershipProjection> findProjectionsByPolityIdAndStatusOrderByAdmittedAtAscIdAsc(
      UUID polityId, MembershipStatus status, Pageable pageable);

  Optional<MembershipProjection> findProjectedById(UUID id);

  Optional<MembershipProjection> findProjectedByPolityIdAndUserIdAndStatus(
      UUID polityId, UUID userId, MembershipStatus status);

  boolean existsByPolityIdAndUserIdAndStatus(UUID polityId, UUID userId, MembershipStatus status);

  boolean existsByPolityIdAndEmailIgnoreCaseAndStatus(
      UUID polityId, String email, MembershipStatus status);

  List<MembershipProjection> findProjectionsByPolityIdAndIdIn(UUID polityId, Collection<UUID> ids);

  long countByPolityIdAndStatus(UUID polityId, MembershipStatus status);
}
