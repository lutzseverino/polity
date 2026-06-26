package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionalReviewProposal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionalReviewProposalRepository
    extends JpaRepository<ConstitutionalReviewProposal, UUID> {
  Optional<ConstitutionalReviewProposalProjection> findProjectedByMotionId(UUID motionId);

  List<ConstitutionalReviewProposalProjection> findProjectionsByPolityIdAndTargetRecordId(
      UUID polityId, UUID targetRecordId);
}
