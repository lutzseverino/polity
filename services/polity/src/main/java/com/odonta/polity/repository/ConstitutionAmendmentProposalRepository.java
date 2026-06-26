package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionAmendmentProposal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionAmendmentProposalRepository
    extends JpaRepository<ConstitutionAmendmentProposal, UUID> {
  Optional<ConstitutionAmendmentProposalProjection> findProjectedByMotionId(UUID motionId);
}
