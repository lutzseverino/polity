package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeElectionProposal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeElectionProposalRepository
    extends JpaRepository<OfficeElectionProposal, UUID> {
  Optional<OfficeElectionProposalProjection> findProjectedByMotionId(UUID motionId);

  List<OfficeElectionProposalProjection> findProjectionsByPolityIdAndMotionIdIn(
      UUID polityId, Collection<UUID> motionIds);
}
