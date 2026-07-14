package com.odonta.polity.repository;

import com.odonta.polity.model.OfficialRecordEntry;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialRecordRepository extends JpaRepository<OfficialRecordEntry, UUID> {

  Optional<OfficialRecordEntry> findEntityByIdAndPolityId(UUID id, UUID polityId);

  Optional<OfficialRecordProjection> findProjectedByIdAndPolityId(UUID id, UUID polityId);

  List<OfficialRecordProjection> findProjectionsByPolityIdAndIdIn(
      UUID polityId, Collection<UUID> ids);

  Page<OfficialRecordProjection> findProjectionsByPolityIdOrderByEntryNumberDescIdAsc(
      UUID polityId, Pageable pageable);
}
