package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OfficeTermRepository extends JpaRepository<OfficeTerm, UUID> {
  boolean existsByPolityIdAndOfficeCodeAndMembershipIdAndStatusAndEndsAtAfter(
      UUID polityId,
      String officeCode,
      UUID membershipId,
      OfficeTermStatus status,
      OffsetDateTime now);

  List<OfficeTerm> findByPolityIdAndOfficeCodeAndStatus(
      UUID polityId, String officeCode, OfficeTermStatus status);

  @Query(
      """
      select
        term.id as id,
        office.id as officeId,
        office.name as officeName,
        member.id as membershipId,
        member.displayName as memberName,
        term.status as status,
        term.startedAt as startedAt,
        term.endsAt as endsAt
      from OfficeTerm term
      join Office office on office.id = term.officeId
      join Membership member on member.id = term.membershipId
      where term.polityId = :polityId
      order by term.startedAt desc
      """)
  List<OfficeTermProjection> findProjectionsByPolityId(UUID polityId);
}
