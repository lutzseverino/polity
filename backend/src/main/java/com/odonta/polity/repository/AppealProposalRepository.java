package com.odonta.polity.repository;

import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.MotionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppealProposalRepository extends JpaRepository<AppealProposal, UUID> {
  Optional<AppealProposal> findByMotionId(UUID motionId);

  @Query(
      """
      select count(proposal) > 0
      from AppealProposal proposal
      join Motion motion on motion.id = proposal.motionId
      where proposal.polityId = :polityId
        and proposal.sanctionId = :sanctionId
        and motion.status = :status
      """)
  boolean existsByPolityIdAndSanctionIdAndMotionStatus(
      UUID polityId, UUID sanctionId, MotionStatus status);
}
