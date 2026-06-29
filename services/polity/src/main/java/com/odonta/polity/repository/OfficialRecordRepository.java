package com.odonta.polity.repository;

import com.odonta.polity.model.OfficialRecordEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialRecordRepository extends JpaRepository<OfficialRecordEntry, UUID> {

  Optional<OfficialRecordEntry> findEntityByIdAndPolityId(UUID id, UUID polityId);

  Optional<OfficialRecordProjection> findProjectedByIdAndPolityId(UUID id, UUID polityId);

  List<OfficialRecordProjection> findProjectionsByPolityIdOrderByEntryNumberDesc(UUID polityId);
}
