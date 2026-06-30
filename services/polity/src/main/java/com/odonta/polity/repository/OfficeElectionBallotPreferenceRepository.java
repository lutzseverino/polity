package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeElectionBallotPreference;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeElectionBallotPreferenceRepository
    extends JpaRepository<OfficeElectionBallotPreference, UUID> {
  void deleteByBallotId(UUID ballotId);

  List<OfficeElectionBallotPreference> findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(
      UUID motionId);

  List<OfficeElectionBallotPreference> findEntitiesByMotionIdAndMembershipIdOrderByRankAsc(
      UUID motionId, UUID membershipId);
}
