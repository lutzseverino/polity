package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficeTermApplicationMapper;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficeTermRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class OfficeTermServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final MembershipService memberships = mock(MembershipService.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository terms = mock(OfficeTermRepository.class);
  private final OfficeTermService service =
      new OfficeTermService(
          Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
          access,
          Mappers.getMapper(OfficeTermApplicationMapper.class),
          memberships,
          offices,
          terms);

  @Test
  void expiredActiveOfficeTermsReadAsEnded() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    OfficeTermProjection projection = activeTermProjection(NOW.minusMinutes(1));
    UUID officeId = projection.getOfficeId();
    UUID membershipId = projection.getMembershipId();
    OfficeProjection office = mock(OfficeProjection.class);

    when(office.getId()).thenReturn(officeId);
    when(office.getName()).thenReturn("Steward");
    when(office.getNameKey()).thenReturn("office.steward.name");
    when(offices.findProjectionsByPolityIdAndIdIn(polityId, List.of(officeId)))
        .thenReturn(List.of(office));
    when(memberships.displayNames(polityId, List.of(membershipId)))
        .thenReturn(Map.of(membershipId, "Ada"));
    when(terms.findProjectionsByPolityIdOrderByStartedAtDescIdAsc(
            org.mockito.ArgumentMatchers.eq(polityId),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(projection)));

    var results = service.list(polityId, userId, 0, 50).items();

    assertThat(results).singleElement().extracting("status").isEqualTo(OfficeTermStatus.ENDED);
  }

  @Test
  void officeTermsPreservePersistedOfficeIdentityAndHistoricalMetadata() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID historicalOfficeId = UUID.randomUUID();
    OfficeTermProjection projection = activeTermProjection(NOW.plusDays(1));
    String officeCode = projection.getOfficeCode();
    UUID membershipId = projection.getMembershipId();
    OfficeProjection historicalOffice = mock(OfficeProjection.class);

    when(projection.getOfficeId()).thenReturn(historicalOfficeId);
    when(historicalOffice.getId()).thenReturn(historicalOfficeId);
    when(historicalOffice.getCode()).thenReturn(officeCode);
    when(historicalOffice.getName()).thenReturn("Steward");
    when(historicalOffice.getNameKey()).thenReturn("office.steward.name");
    when(offices.findProjectionsByPolityIdAndIdIn(polityId, List.of(historicalOfficeId)))
        .thenReturn(List.of(historicalOffice));
    when(memberships.displayNames(polityId, List.of(membershipId)))
        .thenReturn(Map.of(membershipId, "Ada"));
    when(terms.findProjectionsByPolityIdOrderByStartedAtDescIdAsc(
            org.mockito.ArgumentMatchers.eq(polityId),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(projection)));

    var results = service.list(polityId, userId, 0, 50).items();

    assertThat(results)
        .singleElement()
        .satisfies(
            result -> {
              assertThat(result.officeId()).isEqualTo(historicalOfficeId);
              assertThat(result.officeName()).isEqualTo("Steward");
              assertThat(result.officeNameKey()).isEqualTo("office.steward.name");
            });
    verify(offices, never()).findProjectionsByConstitutionVersionIdOrderByNameAscIdAsc(any());
  }

  private OfficeTermProjection activeTermProjection(OffsetDateTime endsAt) {
    OfficeTermProjection projection = mock(OfficeTermProjection.class);
    when(projection.getId()).thenReturn(UUID.randomUUID());
    when(projection.getOfficeId()).thenReturn(UUID.randomUUID());
    when(projection.getOfficeCode()).thenReturn("steward");
    when(projection.getMembershipId()).thenReturn(UUID.randomUUID());
    when(projection.getStatus()).thenReturn(OfficeTermStatus.ACTIVE);
    when(projection.getStartedAt()).thenReturn(NOW.minusDays(7));
    when(projection.getEndsAt()).thenReturn(endsAt);
    return projection;
  }
}
