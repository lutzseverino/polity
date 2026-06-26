package com.odonta.polity.repository;

import com.odonta.polity.model.SanctionProposal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanctionProposalRepository extends JpaRepository<SanctionProposal, UUID> {
  Optional<SanctionProposalProjection> findProjectedByMotionId(UUID motionId);
}
