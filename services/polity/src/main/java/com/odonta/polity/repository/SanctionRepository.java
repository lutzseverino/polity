package com.odonta.polity.repository;

import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanctionRepository extends JpaRepository<Sanction, UUID> {
  Optional<Sanction> findEntityByIdAndPolityId(UUID id, UUID polityId);

  boolean existsByPolityIdAndTargetMembershipIdAndTypeAndStatusAndEndsAtAfter(
      UUID polityId,
      UUID targetMembershipId,
      SanctionType type,
      SanctionStatus status,
      OffsetDateTime now);

  Page<SanctionProjection> findProjectionsByPolityIdOrderByStartedAtDescIdAsc(
      UUID polityId, Pageable pageable);

  List<SanctionProjection>
      findProjectionsByPolityIdAndTargetMembershipIdInAndTypeAndStatusAndEndsAtAfter(
          UUID polityId,
          Collection<UUID> targetMembershipIds,
          SanctionType type,
          SanctionStatus status,
          OffsetDateTime now);

  List<SanctionProjection>
      findProjectionsByPolityIdAndTargetMembershipIdAndStatusAndEndsAtAfterOrderByStartedAtDesc(
          UUID polityId, UUID targetMembershipId, SanctionStatus status, OffsetDateTime now);
}
