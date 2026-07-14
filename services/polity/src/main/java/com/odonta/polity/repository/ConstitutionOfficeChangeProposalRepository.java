package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionOfficeChangeProposalRepository
    extends JpaRepository<ConstitutionOfficeChangeProposal, UUID> {

  List<ConstitutionOfficeChangeProposalProjection> findProjectionsByAmendmentProposalId(
      UUID amendmentProposalId);

  List<ConstitutionOfficeChangeProposalProjection>
      findProjectionsByPolityIdAndAmendmentProposalIdIn(
          UUID polityId, Collection<UUID> amendmentProposalIds);
}
