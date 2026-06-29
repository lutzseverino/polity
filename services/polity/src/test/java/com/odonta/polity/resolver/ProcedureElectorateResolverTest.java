package com.odonta.polity.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.service.MembershipService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProcedureElectorateResolverTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-22T10:00:00Z");

  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final MembershipService membershipService = mock(MembershipService.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final ProcedureElectorateResolver service =
      new ProcedureElectorateResolver(memberships, membershipService, officeTerms);

  @Test
  void activeMemberElectorateUsesStandingMembers() {
    UUID polityId = UUID.randomUUID();
    Membership standing = member(polityId);
    Membership suspended = member(polityId);
    Procedure procedure = activeMemberProcedure(polityId);

    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(standing, suspended));
    when(membershipService.hasPoliticalStanding(standing.getId(), NOW)).thenReturn(true);
    when(membershipService.hasPoliticalStanding(suspended.getId(), NOW)).thenReturn(false);

    assertThat(service.electors(procedure, NOW)).containsExactly(standing);
  }

  @Test
  void officeHolderElectorateUsesStandingOfficeHolders() {
    UUID polityId = UUID.randomUUID();
    UUID officeId = UUID.randomUUID();
    Membership magistrate = member(polityId);
    OfficeTerm term =
        new OfficeTerm(
            polityId,
            officeId,
            Office.MAGISTRATE,
            magistrate.getId(),
            NOW.minusDays(1),
            NOW.plusDays(10));
    Procedure procedure = officeHolderProcedure(polityId);

    when(officeTerms.findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
            polityId, Office.MAGISTRATE, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(List.of(term));
    when(memberships.findEntityById(magistrate.getId()))
        .thenReturn(java.util.Optional.of(magistrate));
    when(membershipService.hasPoliticalStanding(magistrate.getId(), NOW)).thenReturn(true);

    assertThat(service.electors(procedure, NOW)).containsExactly(magistrate);
  }

  private Procedure activeMemberProcedure(UUID polityId) {
    return new Procedure(
        polityId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        Procedure.ORDINARY_RESOLUTION,
        "Ordinary resolution",
        1,
        2,
        VotingThreshold.SIMPLE_MAJORITY_CAST,
        0,
        24,
        com.odonta.polity.model.EffectType.ADOPT_RESOLUTION);
  }

  private Procedure officeHolderProcedure(UUID polityId) {
    return new Procedure(
        polityId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        Procedure.APPEAL,
        "Appeal",
        null,
        1,
        2,
        VotingThreshold.SIMPLE_MAJORITY_CAST,
        ProcedureElectorate.OFFICE_HOLDERS,
        Office.MAGISTRATE,
        0,
        24,
        com.odonta.polity.model.EffectType.GRANT_APPEAL);
  }

  private Membership member(UUID polityId) {
    Membership member =
        new Membership(
            polityId,
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            "citizen@example.test",
            "Citizen",
            NOW.minusDays(2),
            null);
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }
}
