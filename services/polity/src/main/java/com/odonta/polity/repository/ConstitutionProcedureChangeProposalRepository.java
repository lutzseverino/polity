package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionProcedureChangeProposalRepository
    extends JpaRepository<ConstitutionProcedureChangeProposal, UUID> {

  List<ConstitutionProcedureChangeProposalProjection> findProjectionsByAmendmentProposalId(
      UUID amendmentProposalId);

  List<ConstitutionProcedureChangeProposalProjection>
      findProjectionsByPolityIdAndAmendmentProposalIdIn(
          UUID polityId, Collection<UUID> amendmentProposalIds);
}
