package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeElectionBallot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeElectionBallotRepository extends JpaRepository<OfficeElectionBallot, UUID> {
  Optional<OfficeElectionBallot> findByMotionIdAndMembershipId(UUID motionId, UUID membershipId);

  List<OfficeElectionBallot> findByMotionId(UUID motionId);
}
