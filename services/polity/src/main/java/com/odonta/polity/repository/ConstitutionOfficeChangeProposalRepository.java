package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionOfficeChangeProposalRepository
    extends JpaRepository<ConstitutionOfficeChangeProposal, UUID> {

  List<ConstitutionOfficeChangeProposal> findByAmendmentProposalId(UUID amendmentProposalId);
}
