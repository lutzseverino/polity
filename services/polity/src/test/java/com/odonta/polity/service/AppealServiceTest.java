package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.AppealApplicationMapper;
import com.odonta.polity.repository.AppealProjection;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.result.AppealResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class AppealServiceTest {
  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final AppealRepository appeals = mock(AppealRepository.class);
  private final AppealApplicationMapper mapper = mock(AppealApplicationMapper.class);
  private final MembershipService memberships = mock(MembershipService.class);
  private final AppealService service = new AppealService(access, appeals, mapper, memberships);

  @Test
  void listResolvesAppellantNamesInOneBatch() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID appellantId = UUID.randomUUID();
    AppealProjection projection = mock(AppealProjection.class);
    AppealResult expected = mock(AppealResult.class);
    when(projection.getAppellantMembershipId()).thenReturn(appellantId);
    when(appeals.findProjectionsByPolityIdOrderByDecidedAtDescIdAsc(
            org.mockito.ArgumentMatchers.eq(polityId),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(projection)));
    when(memberships.displayNames(polityId, List.of(appellantId)))
        .thenReturn(Map.of(appellantId, "Ada"));
    when(mapper.toResult(projection, "Ada")).thenReturn(expected);

    assertThat(service.list(polityId, userId, 0, 50).items()).containsExactly(expected);
    verify(access).requireReadable(polityId, userId);
    verify(memberships).displayNames(polityId, List.of(appellantId));
  }
}
