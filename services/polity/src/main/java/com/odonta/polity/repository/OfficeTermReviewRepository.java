package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeTermReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeTermReviewRepository extends JpaRepository<OfficeTermReview, UUID> {
  boolean existsByPolityIdAndOfficeTermId(UUID polityId, UUID officeTermId);

  List<OfficeTermReviewProjection> findProjectionsByPolityIdOrderByDecidedAtDesc(UUID polityId);
}
