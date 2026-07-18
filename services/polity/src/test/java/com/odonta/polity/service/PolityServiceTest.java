package com.odonta.polity.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.resolver.PolitySummaryResolver;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

class PolityServiceTest {
  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final PolitySummaryResolver summaries = mock(PolitySummaryResolver.class);
  private final PolityService service = new PolityService(access, polities, summaries);

  @Test
  void listTrimsThePolityQueryBeforeApplyingPagination() {
    UUID userId = UUID.randomUUID();
    PageRequest pageRequest = PageRequest.of(1, 25);
    when(polities.findAccessibleProjections(
            userId, MembershipStatus.ACTIVE, PolityVisibility.PUBLIC, "assembly", pageRequest))
        .thenReturn(Page.<PolityProjection>empty(pageRequest));
    when(summaries.resolveAll(List.of())).thenReturn(List.of());

    service.list(userId, "  assembly  ", 1, 25);

    verify(polities)
        .findAccessibleProjections(
            userId, MembershipStatus.ACTIVE, PolityVisibility.PUBLIC, "assembly", pageRequest);
  }

  @Test
  void listTreatsABlankPolityQueryAsAbsent() {
    UUID userId = UUID.randomUUID();
    PageRequest pageRequest = PageRequest.of(0, 50);
    when(polities.findAccessibleProjections(
            userId, MembershipStatus.ACTIVE, PolityVisibility.PUBLIC, null, pageRequest))
        .thenReturn(Page.<PolityProjection>empty(pageRequest));
    when(summaries.resolveAll(List.of())).thenReturn(List.of());

    service.list(userId, " \t ", 0, 50);

    verify(polities)
        .findAccessibleProjections(
            userId, MembershipStatus.ACTIVE, PolityVisibility.PUBLIC, null, pageRequest);
  }
}
