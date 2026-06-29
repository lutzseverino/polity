package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionalReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionalReviewRepository extends JpaRepository<ConstitutionalReview, UUID> {
  boolean existsByPolityIdAndTargetRecordId(UUID polityId, UUID targetRecordId);

  List<ConstitutionalReviewProjection> findProjectionsByPolityIdOrderByDecidedAtDesc(UUID polityId);
}
