package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionInstitutionChangeProposal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionInstitutionChangeProposalRepository
    extends JpaRepository<ConstitutionInstitutionChangeProposal, UUID> {
  List<ConstitutionInstitutionChangeProposalProjection> findProjectionsByAmendmentProposalId(
      UUID amendmentProposalId);
}
