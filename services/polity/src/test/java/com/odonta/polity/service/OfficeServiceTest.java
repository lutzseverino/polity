package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficeApplicationMapper;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficeTermRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OfficeServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final MembershipService memberships = mock(MembershipService.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository terms = mock(OfficeTermRepository.class);
  private final OfficeService service =
      new OfficeService(
          Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
          access,
          Mappers.getMapper(OfficeApplicationMapper.class),
          memberships,
          offices,
          terms,
          mock(PolityService.class));

  @Test
  void expiredActiveOfficeTermsReadAsEnded() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    OfficeTermProjection projection = activeTermProjection(NOW.minusMinutes(1));
    OfficeProjection office = mock(OfficeProjection.class);

    when(office.getName()).thenReturn("Steward");
    when(office.getNameKey()).thenReturn("office.steward.name");
    when(offices.findProjectedById(projection.getOfficeId()))
        .thenReturn(java.util.Optional.of(office));
    when(memberships.displayName(projection.getMembershipId())).thenReturn("Ada");
    when(terms.findProjectionsByPolityIdOrderByStartedAtDesc(polityId))
        .thenReturn(List.of(projection));

    var results = service.terms(polityId, userId);

    assertThat(results).singleElement().extracting("status").isEqualTo(OfficeTermStatus.ENDED);
  }

  private OfficeTermProjection activeTermProjection(OffsetDateTime endsAt) {
    OfficeTermProjection projection = mock(OfficeTermProjection.class);
    when(projection.getId()).thenReturn(UUID.randomUUID());
    when(projection.getOfficeId()).thenReturn(UUID.randomUUID());
    when(projection.getMembershipId()).thenReturn(UUID.randomUUID());
    when(projection.getStatus()).thenReturn(OfficeTermStatus.ACTIVE);
    when(projection.getStartedAt()).thenReturn(NOW.minusDays(7));
    when(projection.getEndsAt()).thenReturn(endsAt);
    return projection;
  }
}
