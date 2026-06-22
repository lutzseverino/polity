package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficialRecordApplicationMapper;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordSequence;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.OfficialRecordSequenceRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;

class OfficialRecordServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-16T12:00:00Z");

  private final OfficialRecordRepository records = mock(OfficialRecordRepository.class);
  private final OfficialRecordSequenceRepository sequences =
      mock(OfficialRecordSequenceRepository.class);
  private final OfficialRecordService service =
      new OfficialRecordService(
          mock(PolityAccessPolicy.class),
          mock(ConstitutionVersionRepository.class),
          Mappers.getMapper(OfficialRecordApplicationMapper.class),
          mock(MembershipService.class),
          records,
          sequences);

  @Test
  void assignsSequentialEntryNumbersForAPolity() {
    UUID polityId = UUID.randomUUID();
    OfficialRecordSequence sequence = new OfficialRecordSequence(polityId);
    when(sequences.findByPolityIdForUpdate(polityId)).thenReturn(Optional.of(sequence));
    when(records.save(any(OfficialRecordEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    append(polityId);
    append(polityId);

    ArgumentCaptor<OfficialRecordEntry> entries =
        ArgumentCaptor.forClass(OfficialRecordEntry.class);
    verify(records, times(2)).save(entries.capture());
    OfficialRecordEntry first = entries.getAllValues().getFirst();
    OfficialRecordEntry second = entries.getAllValues().get(1);
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

    append(polityId);

    ArgumentCaptor<OfficialRecordEntry> entries =
        ArgumentCaptor.forClass(OfficialRecordEntry.class);
    verify(records).save(entries.capture());
    OfficialRecordEntry entry = entries.getValue();
    assertThat(entry.getEntryNumber()).isEqualTo(1);
  }

  @Test
  void storesStructuredMessageMetadata() {
    UUID polityId = UUID.randomUUID();
    when(sequences.findByPolityIdForUpdate(polityId))
        .thenReturn(Optional.of(new OfficialRecordSequence(polityId)));
    when(records.save(any(OfficialRecordEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.append(
        polityId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        OfficialRecordType.POLITY_FOUNDED,
        polityId,
        OfficialRecordContext.none(),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.POLITY_FOUNDED,
            Map.of(
                "polityName", "Tiny Senate", "setupPreset", "standard republic", "pace", "fast")),
        NOW);

    ArgumentCaptor<OfficialRecordEntry> entries =
        ArgumentCaptor.forClass(OfficialRecordEntry.class);
    verify(records).save(entries.capture());
    OfficialRecordEntry entry = entries.getValue();
    assertThat(entry.getTitleKey()).isEqualTo("official_record.polity_founded.title");
    assertThat(entry.getBodyKey()).isEqualTo("official_record.polity_founded.body");
    assertThat(entry.getTitle()).isEqualTo("Tiny Senate was founded");
    assertThat(entry.getBody())
        .isEqualTo("The polity was founded with the standard republic preset and fast pace.");
    assertThat(entry.getTemplateParams()).containsEntry("polityName", "Tiny Senate");
  }

  private void append(UUID polityId) {
    service.append(
        polityId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        OfficialRecordType.POLITY_FOUNDED,
        polityId,
        OfficialRecordContext.none(),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.POLITY_FOUNDED,
            Map.of(
                "polityName", "Tiny Senate", "setupPreset", "standard republic", "pace", "fast")),
        NOW);
  }
}
