package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeTermReviewProposal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeTermReviewProposalRepository
    extends JpaRepository<OfficeTermReviewProposal, UUID> {
  Optional<OfficeTermReviewProposalProjection> findProjectedByMotionId(UUID motionId);

  List<OfficeTermReviewProposalProjection> findProjectionsByPolityIdAndOfficeTermId(
      UUID polityId, UUID officeTermId);
}
