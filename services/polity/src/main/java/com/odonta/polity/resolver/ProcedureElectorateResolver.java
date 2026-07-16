package com.odonta.polity.resolver;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.service.MembershipService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcedureElectorateResolver {
  private final MembershipRepository memberships;
  private final MembershipService membershipService;
  private final OfficeTermRepository officeTerms;

  public List<Membership> electors(Procedure procedure, OffsetDateTime votingOpensAt) {
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
        .filter(member -> membershipService.hasPoliticalStanding(member.getId(), votingOpensAt))
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
        .map(term -> membership(term.getMembershipId()))
        .filter(member -> membershipService.hasPoliticalStanding(member.getId(), votingOpensAt))
        .toList();
  }

  private Membership membership(java.util.UUID membershipId) {
    return memberships.findEntityById(membershipId).orElseThrow(PolityResource.MEMBER::notFound);
  }
}
