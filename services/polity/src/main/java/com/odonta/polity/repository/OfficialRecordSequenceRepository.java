package com.odonta.polity.repository;

import com.odonta.polity.model.OfficialRecordSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OfficialRecordSequenceRepository
    extends JpaRepository<OfficialRecordSequence, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select recordSequence
      from OfficialRecordSequence recordSequence
      where recordSequence.polityId = :polityId
      """)
  Optional<OfficialRecordSequence> findByPolityIdForUpdate(UUID polityId);
}
