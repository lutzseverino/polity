package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.ConstitutionalReviewApplicationMapper;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.repository.ConstitutionalReviewProjection;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.OfficialRecordProjection;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.result.ConstitutionalReviewResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class ConstitutionalReviewServiceTest {
  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final ConstitutionalReviewRepository reviews = mock(ConstitutionalReviewRepository.class);
  private final ConstitutionalReviewApplicationMapper mapper =
      mock(ConstitutionalReviewApplicationMapper.class);
  private final MembershipService memberships = mock(MembershipService.class);
  private final OfficialRecordRepository officialRecords = mock(OfficialRecordRepository.class);
  private final ConstitutionalReviewService service =
      new ConstitutionalReviewService(access, reviews, mapper, memberships, officialRecords);

  @Test
  void listBatchesTargetRecordsAndPetitionerNames() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID recordId = UUID.randomUUID();
    UUID petitionerId = UUID.randomUUID();
    ConstitutionalReviewProjection review = mock(ConstitutionalReviewProjection.class);
    OfficialRecordProjection record = mock(OfficialRecordProjection.class);
    ConstitutionalReviewResult expected = mock(ConstitutionalReviewResult.class);
    when(review.getTargetRecordId()).thenReturn(recordId);
    when(review.getPetitionerMembershipId()).thenReturn(petitionerId);
    when(record.getId()).thenReturn(recordId);
    when(record.getEntryNumber()).thenReturn(7);
    when(record.getType()).thenReturn(OfficialRecordType.RESOLUTION_ADOPTED);
    when(reviews.findProjectionsByPolityIdOrderByDecidedAtDescIdAsc(
            org.mockito.ArgumentMatchers.eq(polityId),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(review)));
    when(officialRecords.findProjectionsByPolityIdAndIdIn(polityId, List.of(recordId)))
        .thenReturn(List.of(record));
    when(memberships.displayNames(polityId, List.of(petitionerId)))
        .thenReturn(Map.of(petitionerId, "Ada"));
    when(mapper.toResult(review, 7, OfficialRecordType.RESOLUTION_ADOPTED, "Ada"))
        .thenReturn(expected);

    assertThat(service.list(polityId, userId, 0, 50).items()).containsExactly(expected);
    verify(access).requireReadable(polityId, userId);
    verify(officialRecords).findProjectionsByPolityIdAndIdIn(polityId, List.of(recordId));
    verify(memberships).displayNames(polityId, List.of(petitionerId));
  }
}
