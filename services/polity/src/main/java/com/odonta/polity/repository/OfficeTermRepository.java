package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeTermRepository extends JpaRepository<OfficeTerm, UUID> {
  boolean existsByPolityIdAndOfficeCodeAndMembershipIdAndStatusAndEndsAtAfter(
      UUID polityId,
      String officeCode,
      UUID membershipId,
      OfficeTermStatus status,
      OffsetDateTime now);

  boolean existsByPolityIdAndOfficeCodeAndMembershipIdAndStatusAndAssignedByMotionIdIsNull(
      UUID polityId, String officeCode, UUID membershipId, OfficeTermStatus status);

  boolean existsByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
      UUID polityId, String officeCode, OfficeTermStatus status, OffsetDateTime now);

  long countByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
      UUID polityId, String officeCode, OfficeTermStatus status, OffsetDateTime now);

  Optional<OfficeTerm> findEntityByIdAndPolityId(UUID id, UUID polityId);

  Optional<OfficeTermProjection> findProjectedByIdAndPolityId(UUID id, UUID polityId);

  List<OfficeTerm> findEntitiesByPolityIdAndOfficeCodeAndStatus(
      UUID polityId, String officeCode, OfficeTermStatus status);

  List<OfficeTerm> findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
      UUID polityId, String officeCode, OfficeTermStatus status, OffsetDateTime now);

  List<OfficeTerm> findEntitiesByPolityIdAndOfficeCodeAndStatusAndAssignedByMotionIdIsNull(
      UUID polityId, String officeCode, OfficeTermStatus status);

  List<OfficeTerm> findEntitiesByPolityIdAndMembershipIdAndStatus(
      UUID polityId, UUID membershipId, OfficeTermStatus status);

  List<OfficeTerm> findEntitiesByPolityIdAndStatus(UUID polityId, OfficeTermStatus status);

  List<OfficeTermProjection> findProjectionsByPolityIdOrderByStartedAtDesc(UUID polityId);
}
