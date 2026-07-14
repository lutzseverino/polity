package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.mapper.MembershipApplicationMapper;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.SanctionProjection;
import com.odonta.polity.repository.SanctionRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipServiceTest {
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private final MembershipService service =
      new MembershipService(mock(MembershipApplicationMapper.class), memberships, sanctions);

  @Test
  void politicalStandingUsesTargetedReadsAndNormalizesMembershipIds() {
    UUID polityId = UUID.randomUUID();
    UUID standingId = UUID.randomUUID();
    UUID suspendedId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.of(2026, 7, 12, 12, 0, 0, 0, ZoneOffset.UTC);
    List<UUID> uniqueIds = List.of(standingId, suspendedId);
    MembershipProjection standing = membership(standingId, MembershipStatus.ACTIVE);
    MembershipProjection suspended = membership(suspendedId, MembershipStatus.ACTIVE);
    SanctionProjection suspension = mock(SanctionProjection.class);
    when(suspension.getTargetMembershipId()).thenReturn(suspendedId);
    when(sanctions.findProjectionsByPolityIdAndTargetMembershipIdInAndTypeAndStatusAndEndsAtAfter(
            polityId, uniqueIds, SanctionType.SUSPENSION, SanctionStatus.ACTIVE, now))
        .thenReturn(List.of(suspension));
    when(memberships.findProjectionsByPolityIdAndIdIn(polityId, uniqueIds))
        .thenReturn(List.of(standing, suspended));

    assertThat(
            service.politicalStanding(polityId, List.of(standingId, suspendedId, standingId), now))
        .isEqualTo(Set.of(standingId));

    verify(sanctions)
        .findProjectionsByPolityIdAndTargetMembershipIdInAndTypeAndStatusAndEndsAtAfter(
            polityId, uniqueIds, SanctionType.SUSPENSION, SanctionStatus.ACTIVE, now);
    verify(memberships).findProjectionsByPolityIdAndIdIn(polityId, uniqueIds);
  }

  private MembershipProjection membership(UUID id, MembershipStatus status) {
    MembershipProjection membership = mock(MembershipProjection.class);
    when(membership.getId()).thenReturn(id);
    when(membership.getStatus()).thenReturn(status);
    return membership;
  }
}
