package com.odonta.polity.service;

import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.repository.OfficialRecordRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficialRecordWriter {
  private final OfficialRecordRepository records;

  public OfficialRecordEntry append(
      UUID polityId,
      UUID jurisdictionId,
      UUID constitutionVersionId,
      UUID actorMembershipId,
      OfficialRecordType type,
      UUID sourceId,
      String title,
      String body,
      OffsetDateTime occurredAt) {
    return records.save(
        new OfficialRecordEntry(
            polityId,
            jurisdictionId,
            constitutionVersionId,
            actorMembershipId,
            type,
            sourceId,
            title,
            body,
            occurredAt));
  }
}
