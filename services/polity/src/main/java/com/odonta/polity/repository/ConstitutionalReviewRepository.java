package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionalReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConstitutionalReviewRepository extends JpaRepository<ConstitutionalReview, UUID> {
  boolean existsByPolityIdAndTargetRecordId(UUID polityId, UUID targetRecordId);

  @Query(
      """
      select review.id as id,
        review.targetRecordId as targetRecordId,
        target.entryNumber as targetEntryNumber,
        target.type as targetType,
        review.petitionerMembershipId as petitionerMembershipId,
        review.status as status,
        review.reason as reason,
        review.decidedAt as decidedAt
      from ConstitutionalReview review
      join OfficialRecordEntry target
        on target.polityId = review.polityId
        and target.id = review.targetRecordId
      where review.polityId = :polityId
      order by review.decidedAt desc
      """)
  List<ConstitutionalReviewProjection> findProjectionsByPolityIdOrderByDecidedAtDesc(UUID polityId);
}
