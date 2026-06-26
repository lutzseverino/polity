package com.odonta.polity.service;

import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ProcedureElectorateService {
  private final MembershipRepository memberships;
  private final MembershipService membershipService;
  private final OfficeTermRepository officeTerms;

  List<Membership> electors(Procedure procedure, OffsetDateTime votingOpensAt) {
    return switch (procedure.getElectorate()) {
      case ACTIVE_MEMBERS -> activeMembers(procedure, votingOpensAt);
      case OFFICE_HOLDERS -> officeHolders(procedure, votingOpensAt);
    };
  }

  private List<Membership> activeMembers(Procedure procedure, OffsetDateTime votingOpensAt) {
    return memberships
        .findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            procedure.getPolityId(), MembershipStatus.ACTIVE)
        .stream()
        .filter(member -> membershipService.hasPoliticalStanding(member, votingOpensAt))
        .toList();
  }

  private List<Membership> officeHolders(Procedure procedure, OffsetDateTime votingOpensAt) {
    return officeTerms
        .findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
            procedure.getPolityId(),
            procedure.getElectorateOfficeCode(),
            OfficeTermStatus.ACTIVE,
            votingOpensAt)
        .stream()
        .map(term -> membershipService.get(term.getMembershipId()))
        .filter(member -> membershipService.hasPoliticalStanding(member, votingOpensAt))
        .toList();
  }
}
