package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeAssignmentProposal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeAssignmentProposalRepository
    extends JpaRepository<OfficeAssignmentProposal, UUID> {
  Optional<OfficeAssignmentProposal> findByMotionId(UUID motionId);
}
