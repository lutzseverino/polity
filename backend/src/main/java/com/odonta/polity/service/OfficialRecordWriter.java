package com.odonta.polity.service;

import com.odonta.polity.model.OfficialRecordCitation;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordSequence;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.OfficialRecordSequenceRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OfficialRecordWriter {
  private final OfficialRecordRepository records;
  private final OfficialRecordSequenceRepository sequences;

  @Transactional(propagation = Propagation.MANDATORY)
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
    return append(
        polityId,
        jurisdictionId,
        constitutionVersionId,
        actorMembershipId,
        type,
        sourceId,
        OfficialRecordCitation.empty(),
        title,
        body,
        occurredAt);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public OfficialRecordEntry append(
      UUID polityId,
      UUID jurisdictionId,
      UUID constitutionVersionId,
      UUID actorMembershipId,
      OfficialRecordType type,
      UUID sourceId,
      OfficialRecordCitation citation,
      String title,
      String body,
      OffsetDateTime occurredAt) {
    int entryNumber = nextEntryNumber(polityId);
    return records.save(
        new OfficialRecordEntry(
            polityId,
            entryNumber,
            jurisdictionId,
            constitutionVersionId,
            actorMembershipId,
            type,
            sourceId,
            citation,
            title,
            body,
            occurredAt));
  }

  private int nextEntryNumber(UUID polityId) {
    OfficialRecordSequence sequence =
        sequences
            .findByPolityIdForUpdate(polityId)
            .orElseGet(() -> sequences.saveAndFlush(new OfficialRecordSequence(polityId)));
    return sequence.claimNextEntryNumber();
  }
}
