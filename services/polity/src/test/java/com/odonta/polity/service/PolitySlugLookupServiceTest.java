package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.result.PolitySummaryResult;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolitySlugLookupServiceTest {
  private final PolityRepository polityRepository = mock(PolityRepository.class);
  private final PolityService polities = mock(PolityService.class);
  private final PolitySlugLookupService service =
      new PolitySlugLookupService(polityRepository, polities);

  @Test
  void delegatesTheResolvedIdToThePermissionSecuredPolityLookup() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    PolityProjection projection = mock(PolityProjection.class);
    PolitySummaryResult summary = mock(PolitySummaryResult.class);
    when(projection.getId()).thenReturn(polityId);
    when(polityRepository.findProjectedBySlug("thursday-assembly"))
        .thenReturn(Optional.of(projection));
    when(polities.get(polityId, userId)).thenReturn(summary);

    assertThat(service.get("thursday-assembly", userId)).isSameAs(summary);

    verify(polities).get(polityId, userId);
  }
}
