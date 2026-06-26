package com.odonta.polity.repository;

import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
  Optional<Membership> findEntityById(UUID id);

  Optional<Membership> findEntityByPolityIdAndUserIdAndStatus(
      UUID polityId, UUID userId, MembershipStatus status);

  boolean existsByPolityIdAndUserId(UUID polityId, UUID userId);

  List<Membership> findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
      UUID polityId, MembershipStatus status);

  List<MembershipProjection> findProjectionsByPolityIdAndStatusOrderByAdmittedAtAsc(
      UUID polityId, MembershipStatus status);

  Optional<MembershipProjection> findProjectedById(UUID id);

  long countByPolityIdAndStatus(UUID polityId, MembershipStatus status);
}
