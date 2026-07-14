package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeTermReview;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeTermReviewRepository extends JpaRepository<OfficeTermReview, UUID> {
  boolean existsByPolityIdAndOfficeTermId(UUID polityId, UUID officeTermId);

  Page<OfficeTermReviewProjection> findProjectionsByPolityIdOrderByDecidedAtDescIdAsc(
      UUID polityId, Pageable pageable);
}
