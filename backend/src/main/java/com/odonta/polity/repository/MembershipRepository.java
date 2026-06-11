package com.odonta.polity.repository;

import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
  Optional<Membership> findByPolityIdAndUserIdAndStatus(
      UUID polityId, UUID userId, MembershipStatus status);

  boolean existsByPolityIdAndUserId(UUID polityId, UUID userId);

  List<Membership> findByPolityIdAndStatusOrderByAdmittedAtAsc(
      UUID polityId, MembershipStatus status);

  List<Membership> findByUserIdAndStatus(UUID userId, MembershipStatus status);

  long countByPolityIdAndStatus(UUID polityId, MembershipStatus status);
}
