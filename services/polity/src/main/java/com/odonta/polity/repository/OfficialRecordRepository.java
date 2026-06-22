package com.odonta.polity.repository;

import com.odonta.polity.model.OfficialRecordEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialRecordRepository extends JpaRepository<OfficialRecordEntry, UUID> {

  List<OfficialRecordProjection> findProjectionsByPolityIdOrderByEntryNumberDesc(UUID polityId);
}
