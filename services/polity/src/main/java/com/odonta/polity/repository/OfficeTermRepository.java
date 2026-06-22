package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeTermRepository extends JpaRepository<OfficeTerm, UUID> {
  boolean existsByPolityIdAndOfficeCodeAndMembershipIdAndStatusAndEndsAtAfter(
      UUID polityId,
      String officeCode,
      UUID membershipId,
      OfficeTermStatus status,
      OffsetDateTime now);

  List<OfficeTerm> findByPolityIdAndOfficeCodeAndStatus(
      UUID polityId, String officeCode, OfficeTermStatus status);

  List<OfficeTerm> findByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
      UUID polityId, String officeCode, OfficeTermStatus status, OffsetDateTime now);

  List<OfficeTerm> findByPolityIdAndOfficeCodeAndStatusAndAssignedByMotionIdIsNull(
      UUID polityId, String officeCode, OfficeTermStatus status);

  List<OfficeTerm> findByPolityIdAndStatus(UUID polityId, OfficeTermStatus status);

  List<OfficeTermProjection> findProjectionsByPolityIdOrderByStartedAtDesc(UUID polityId);
}
