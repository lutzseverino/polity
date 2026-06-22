package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeElectionCandidateRepository
    extends JpaRepository<OfficeElectionCandidate, UUID> {
  List<OfficeElectionCandidate> findByMotionId(UUID motionId);

  List<OfficeElectionCandidate> findByMotionIdAndStatus(
      UUID motionId, OfficeElectionCandidateStatus status);

  Optional<OfficeElectionCandidate> findByMotionIdAndMembershipId(UUID motionId, UUID membershipId);

  boolean existsByMotionIdAndMembershipId(UUID motionId, UUID membershipId);

  boolean existsByMotionIdAndMembershipIdAndStatus(
      UUID motionId, UUID membershipId, OfficeElectionCandidateStatus status);
}
