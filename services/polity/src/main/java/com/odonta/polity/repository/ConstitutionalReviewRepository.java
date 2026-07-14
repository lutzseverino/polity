package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionalReview;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstitutionalReviewRepository extends JpaRepository<ConstitutionalReview, UUID> {
  boolean existsByPolityIdAndTargetRecordId(UUID polityId, UUID targetRecordId);

  Page<ConstitutionalReviewProjection> findProjectionsByPolityIdOrderByDecidedAtDescIdAsc(
      UUID polityId, Pageable pageable);
}
