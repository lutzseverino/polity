package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficeApplicationMapper;
import com.odonta.polity.model.ConstitutionVersion;
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
import org.springframework.test.util.ReflectionTestUtils;

class OfficeServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final MembershipService memberships = mock(MembershipService.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository terms = mock(OfficeTermRepository.class);
  private final PolityService polities = mock(PolityService.class);
  private final OfficeService service =
      new OfficeService(
          Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
          access,
          Mappers.getMapper(OfficeApplicationMapper.class),
          memberships,
          offices,
          terms,
          polities);

  @Test
  void expiredActiveOfficeTermsReadAsEnded() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    OfficeTermProjection projection = activeTermProjection(NOW.minusMinutes(1));
    UUID officeId = projection.getOfficeId();
    OfficeProjection office = mock(OfficeProjection.class);

    when(polities.constitution(polityId)).thenReturn(constitution);
    when(offices.findProjectionsByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of());
    when(office.getId()).thenReturn(officeId);
    when(office.getName()).thenReturn("Steward");
    when(office.getNameKey()).thenReturn("office.steward.name");
    when(offices.findProjectedById(officeId)).thenReturn(java.util.Optional.of(office));
    when(memberships.displayName(projection.getMembershipId())).thenReturn("Ada");
    when(terms.findProjectionsByPolityIdOrderByStartedAtDesc(polityId))
        .thenReturn(List.of(projection));

    var results = service.terms(polityId, userId);

    assertThat(results).singleElement().extracting("status").isEqualTo(OfficeTermStatus.ENDED);
  }

  @Test
  void officeTermsUseCurrentOfficeMetadataByCode() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID currentOfficeId = UUID.randomUUID();
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    OfficeTermProjection projection = activeTermProjection(NOW.plusDays(1));
    String officeCode = projection.getOfficeCode();
    OfficeProjection currentOffice = mock(OfficeProjection.class);

    when(polities.constitution(polityId)).thenReturn(constitution);
    when(currentOffice.getId()).thenReturn(currentOfficeId);
    when(currentOffice.getCode()).thenReturn(officeCode);
    when(currentOffice.getName()).thenReturn("Speaker");
    when(currentOffice.getNameKey()).thenReturn("office.speaker.name");
    when(offices.findProjectionsByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(currentOffice));
    when(memberships.displayName(projection.getMembershipId())).thenReturn("Ada");
    when(terms.findProjectionsByPolityIdOrderByStartedAtDesc(polityId))
        .thenReturn(List.of(projection));

    var results = service.terms(polityId, userId);

    assertThat(results)
        .singleElement()
        .satisfies(
            result -> {
              assertThat(result.officeId()).isEqualTo(currentOfficeId);
              assertThat(result.officeName()).isEqualTo("Speaker");
              assertThat(result.officeNameKey()).isEqualTo("office.speaker.name");
            });
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

  private ConstitutionVersion constitution(UUID polityId, UUID constitutionId) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Charter", "Structured rules", NOW.minusDays(30));
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    return constitution;
  }
}
