package com.odonta.polity.repository;

import com.odonta.polity.model.OfficialRecordEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OfficialRecordRepository extends JpaRepository<OfficialRecordEntry, UUID> {

  @Query(
      """
      select
        entry.id as id,
        entry.type as type,
        entry.title as title,
        entry.body as body,
        actor.displayName as actorName,
        constitution.version as constitutionVersion,
        entry.sourceId as sourceId,
        entry.occurredAt as occurredAt
      from OfficialRecordEntry entry
      join Membership actor on actor.id = entry.actorMembershipId
      join ConstitutionVersion constitution on constitution.id = entry.constitutionVersionId
      where entry.polityId = :polityId
      order by entry.occurredAt desc
      """)
  List<OfficialRecordProjection> findProjectionsByPolityId(UUID polityId);
}
