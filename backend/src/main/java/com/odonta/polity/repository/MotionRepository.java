package com.odonta.polity.repository;

import com.odonta.polity.model.Motion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MotionRepository extends JpaRepository<Motion, UUID> {
  Optional<Motion> findByIdAndPolityId(UUID id, UUID polityId);

  @Query(
      """
      select
        m.id as id,
        m.title as title,
        m.body as body,
        m.status as status,
        m.effectType as effectType,
        c.version as constitutionVersion,
        p.name as procedureName,
        p.quorumNumerator as quorumNumerator,
        p.quorumDenominator as quorumDenominator,
        introducer.displayName as introducedByName,
        m.openedAt as openedAt
      from Motion m
      join ConstitutionVersion c on c.id = m.constitutionVersionId
      join Procedure p on p.id = m.procedureId
      join Membership introducer on introducer.id = m.introducedBy
      where m.polityId = :polityId
      order by m.openedAt desc
      """)
  List<MotionProjection> findProjectionsByPolityId(UUID polityId);

  @Query(
      """
      select
        m.id as id,
        m.title as title,
        m.body as body,
        m.status as status,
        m.effectType as effectType,
        c.version as constitutionVersion,
        p.name as procedureName,
        p.quorumNumerator as quorumNumerator,
        p.quorumDenominator as quorumDenominator,
        introducer.displayName as introducedByName,
        m.openedAt as openedAt
      from Motion m
      join ConstitutionVersion c on c.id = m.constitutionVersionId
      join Procedure p on p.id = m.procedureId
      join Membership introducer on introducer.id = m.introducedBy
      where m.id = :id and m.polityId = :polityId
      """)
  Optional<MotionProjection> findProjectedByIdAndPolityId(UUID id, UUID polityId);
}
