package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.SanctionApplicationMapper;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.repository.SanctionProjection;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.result.SanctionResult;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class SanctionServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-12T08:00:00Z");

  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final SanctionApplicationMapper mapper = mock(SanctionApplicationMapper.class);
  private final MembershipService memberships = mock(MembershipService.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private final SanctionService service =
      new SanctionService(
          access, Clock.fixed(NOW.toInstant(), ZoneOffset.UTC), mapper, memberships, sanctions);

  @Test
  void listResolvesMemberNamesInOneBatch() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID firstMemberId = UUID.randomUUID();
    UUID secondMemberId = UUID.randomUUID();
    SanctionProjection first = sanction(firstMemberId);
    SanctionProjection second = sanction(secondMemberId);
    SanctionResult firstResult = mock(SanctionResult.class);
    SanctionResult secondResult = mock(SanctionResult.class);

    when(sanctions.findProjectionsByPolityIdOrderByStartedAtDescIdAsc(
            org.mockito.ArgumentMatchers.eq(polityId),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(first, second)));
    when(memberships.displayNames(polityId, List.of(firstMemberId, secondMemberId)))
        .thenReturn(Map.of(firstMemberId, "Ada", secondMemberId, "Bea"));
    when(mapper.toResult(first, "Ada", SanctionStatus.ACTIVE)).thenReturn(firstResult);
    when(mapper.toResult(second, "Bea", SanctionStatus.ACTIVE)).thenReturn(secondResult);

    assertThat(service.list(polityId, userId, 0, 50).items())
        .containsExactly(firstResult, secondResult);
    verify(access).requireReadable(polityId, userId);
    verify(memberships).displayNames(polityId, List.of(firstMemberId, secondMemberId));
  }

  @Test
  void listResolvesElapsedActiveSanctionAsExpired() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    SanctionProjection sanction = sanction(memberId);
    SanctionResult expected = mock(SanctionResult.class);
    when(sanction.getEndsAt()).thenReturn(NOW);
    when(sanctions.findProjectionsByPolityIdOrderByStartedAtDescIdAsc(
            org.mockito.ArgumentMatchers.eq(polityId),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(sanction)));
    when(memberships.displayNames(polityId, List.of(memberId))).thenReturn(Map.of(memberId, "Ada"));
    when(mapper.toResult(sanction, "Ada", SanctionStatus.EXPIRED)).thenReturn(expected);

    assertThat(service.list(polityId, userId, 0, 50).items()).containsExactly(expected);
  }

  private SanctionProjection sanction(UUID targetMembershipId) {
    SanctionProjection projection = mock(SanctionProjection.class);
    when(projection.getTargetMembershipId()).thenReturn(targetMembershipId);
    when(projection.getStatus()).thenReturn(SanctionStatus.ACTIVE);
    when(projection.getEndsAt()).thenReturn(NOW.plusDays(1));
    return projection;
  }
}
