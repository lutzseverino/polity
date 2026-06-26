package com.odonta.polity.repository;

import com.odonta.polity.model.Vote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, UUID> {
  Optional<Vote> findEntityByMotionIdAndMembershipId(UUID motionId, UUID membershipId);

  List<Vote> findEntitiesByMotionId(UUID motionId);
}
