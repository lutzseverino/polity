package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficeTermReviewApplicationMapper;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProjection;
import com.odonta.polity.repository.OfficeTermReviewRepository;
import com.odonta.polity.result.OfficeTermReviewResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class OfficeTermReviewServiceTest {
  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final OfficeTermReviewApplicationMapper mapper =
      mock(OfficeTermReviewApplicationMapper.class);
  private final MembershipService memberships = mock(MembershipService.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final OfficeTermReviewRepository reviews = mock(OfficeTermReviewRepository.class);
  private final OfficeTermReviewService service =
      new OfficeTermReviewService(access, mapper, memberships, offices, officeTerms, reviews);

  @Test
  void listBatchesTermsOfficesAndMemberNames() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID termId = UUID.randomUUID();
    UUID officeId = UUID.randomUUID();
    UUID petitionerId = UUID.randomUUID();
    UUID holderId = UUID.randomUUID();
    OfficeTermReviewProjection review = mock(OfficeTermReviewProjection.class);
    OfficeTermProjection term = mock(OfficeTermProjection.class);
    OfficeProjection office = mock(OfficeProjection.class);
    OfficeTermReviewResult expected = mock(OfficeTermReviewResult.class);

    when(review.getOfficeTermId()).thenReturn(termId);
    when(review.getPetitionerMembershipId()).thenReturn(petitionerId);
    when(term.getId()).thenReturn(termId);
    when(term.getOfficeId()).thenReturn(officeId);
    when(term.getMembershipId()).thenReturn(holderId);
    when(office.getId()).thenReturn(officeId);
    when(office.getName()).thenReturn("Steward");
    when(office.getNameKey()).thenReturn("office.steward.name");
    when(reviews.findProjectionsByPolityIdOrderByDecidedAtDescIdAsc(
            org.mockito.ArgumentMatchers.eq(polityId),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(review)));
    when(officeTerms.findProjectionsByPolityIdAndIdIn(polityId, List.of(termId)))
        .thenReturn(List.of(term));
    when(offices.findProjectionsByPolityIdAndIdIn(polityId, List.of(officeId)))
        .thenReturn(List.of(office));
    when(memberships.displayNames(polityId, List.of(petitionerId, holderId)))
        .thenReturn(Map.of(petitionerId, "Ada", holderId, "Bea"));
    when(mapper.toResult(review, "Ada", holderId, "Bea", "Steward", "office.steward.name"))
        .thenReturn(expected);

    assertThat(service.list(polityId, userId, 0, 50).items()).containsExactly(expected);
    verify(access).requireReadable(polityId, userId);
    verify(officeTerms).findProjectionsByPolityIdAndIdIn(polityId, List.of(termId));
    verify(offices).findProjectionsByPolityIdAndIdIn(polityId, List.of(officeId));
    verify(memberships).displayNames(polityId, List.of(petitionerId, holderId));
  }
}
