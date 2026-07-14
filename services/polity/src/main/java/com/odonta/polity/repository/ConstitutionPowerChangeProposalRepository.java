package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionPowerChangeProposal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionPowerChangeProposalRepository
    extends JpaRepository<ConstitutionPowerChangeProposal, UUID> {

  List<ConstitutionPowerChangeProposalProjection> findProjectionsByAmendmentProposalId(
      UUID amendmentProposalId);

  List<ConstitutionPowerChangeProposalProjection> findProjectionsByPolityIdAndAmendmentProposalIdIn(
      UUID polityId, Collection<UUID> amendmentProposalIds);
}
