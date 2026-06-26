package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeTermReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OfficeTermReviewRepository extends JpaRepository<OfficeTermReview, UUID> {
  boolean existsByPolityIdAndOfficeTermId(UUID polityId, UUID officeTermId);

  @Query(
      """
      select review.id as id,
        review.officeTermId as officeTermId,
        review.petitionerMembershipId as petitionerMembershipId,
        term.membershipId as vacatedMembershipId,
        office.name as officeName,
        office.nameKey as officeNameKey,
        review.status as status,
        review.reason as reason,
        review.decidedAt as decidedAt
      from OfficeTermReview review
      join OfficeTerm term
        on term.polityId = review.polityId
        and term.id = review.officeTermId
      join Office office
        on office.polityId = review.polityId
        and office.id = term.officeId
      where review.polityId = :polityId
      order by review.decidedAt desc
      """)
  List<OfficeTermReviewProjection> findProjectionsByPolityIdOrderByDecidedAtDesc(UUID polityId);
}
