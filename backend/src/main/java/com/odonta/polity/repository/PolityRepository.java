package com.odonta.polity.repository;

import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Polity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PolityRepository extends JpaRepository<Polity, UUID> {

  @Query(
      """
      select
        p.id as id,
        p.name as name,
        c.version as constitutionVersion,
        j.name as jurisdictionName,
        i.name as institutionName,
        p.createdAt as createdAt
      from Polity p
      join Membership m on m.polityId = p.id
      join ConstitutionVersion c on c.polityId = p.id and c.status = :constitutionStatus
      join Jurisdiction j on j.polityId = p.id and j.kind = :jurisdictionKind
      join Institution i on i.polityId = p.id and i.kind = :institutionKind
      where m.userId = :userId and m.status = :membershipStatus
      order by p.createdAt desc
      """)
  List<PolityProjection> findProjectionsByMember(
      UUID userId,
      MembershipStatus membershipStatus,
      ConstitutionStatus constitutionStatus,
      JurisdictionKind jurisdictionKind,
      InstitutionKind institutionKind);

  @Query(
      """
      select
        p.id as id,
        p.name as name,
        c.version as constitutionVersion,
        j.name as jurisdictionName,
        i.name as institutionName,
        p.createdAt as createdAt
      from Polity p
      join ConstitutionVersion c on c.polityId = p.id and c.status = :constitutionStatus
      join Jurisdiction j on j.polityId = p.id and j.kind = :jurisdictionKind
      join Institution i on i.polityId = p.id and i.kind = :institutionKind
      where p.id = :id
      """)
  Optional<PolityProjection> findProjectedById(
      UUID id,
      ConstitutionStatus constitutionStatus,
      JurisdictionKind jurisdictionKind,
      InstitutionKind institutionKind);
}
