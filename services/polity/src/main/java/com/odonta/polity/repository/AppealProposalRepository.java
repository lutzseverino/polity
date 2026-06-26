package com.odonta.polity.repository;

import com.odonta.polity.model.AppealProposal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppealProposalRepository extends JpaRepository<AppealProposal, UUID> {
  Optional<AppealProposalProjection> findProjectedByMotionId(UUID motionId);

  List<AppealProposalProjection> findProjectionsByPolityIdAndSanctionId(
      UUID polityId, UUID sanctionId);
}
