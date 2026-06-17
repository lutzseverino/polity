package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordSequence;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.OfficialRecordSequenceRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialRecordWriterTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-16T12:00:00Z");

  private final OfficialRecordRepository records = mock(OfficialRecordRepository.class);
  private final OfficialRecordSequenceRepository sequences =
      mock(OfficialRecordSequenceRepository.class);
  private final OfficialRecordWriter writer = new OfficialRecordWriter(records, sequences);

  @Test
  void assignsSequentialEntryNumbersForAPolity() {
    UUID polityId = UUID.randomUUID();
    OfficialRecordSequence sequence = new OfficialRecordSequence(polityId);
    when(sequences.findByPolityIdForUpdate(polityId)).thenReturn(Optional.of(sequence));
    when(records.save(any(OfficialRecordEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OfficialRecordEntry first = append(polityId);
    OfficialRecordEntry second = append(polityId);

    assertThat(first.getEntryNumber()).isEqualTo(1);
    assertThat(second.getEntryNumber()).isEqualTo(2);
    assertThat(sequence.getNextEntryNumber()).isEqualTo(3);
  }

  @Test
  void createsSequenceWhenMissing() {
    UUID polityId = UUID.randomUUID();
    when(sequences.findByPolityIdForUpdate(polityId)).thenReturn(Optional.empty());
    when(sequences.saveAndFlush(any(OfficialRecordSequence.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(records.save(any(OfficialRecordEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OfficialRecordEntry entry = append(polityId);

    assertThat(entry.getEntryNumber()).isEqualTo(1);
  }

  private OfficialRecordEntry append(UUID polityId) {
    return writer.append(
        polityId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        OfficialRecordType.POLITY_FOUNDED,
        polityId,
        "Polity founded",
        "The polity was founded.",
        NOW);
  }
}
