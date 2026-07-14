package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficialRecordApplicationMapper;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordSequence;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.ConstitutionVersionProjection;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.OfficialRecordProjection;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.OfficialRecordSequenceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class OfficialRecordServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-16T12:00:00Z");

  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final MembershipService memberships = mock(MembershipService.class);
  private final OfficialRecordRepository records = mock(OfficialRecordRepository.class);
  private final OfficialRecordSequenceRepository sequences =
      mock(OfficialRecordSequenceRepository.class);
  private final OfficialRecordService service =
      new OfficialRecordService(
          access,
          constitutions,
          Mappers.getMapper(OfficialRecordApplicationMapper.class),
          memberships,
          records,
          sequences);

  @Test
  void listsRecordsWithBatchedConstitutionAndActorLookups() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID constitutionId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    OfficialRecordProjection first = recordProjection(2, constitutionId, actorId);
    OfficialRecordProjection second = recordProjection(1, constitutionId, actorId);
    ConstitutionVersionProjection constitution = mock(ConstitutionVersionProjection.class);

    when(records.findProjectionsByPolityIdOrderByEntryNumberDescIdAsc(
            org.mockito.ArgumentMatchers.eq(polityId),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(first, second)));
    when(constitutions.findProjectionsByPolityIdAndIdIn(polityId, List.of(constitutionId)))
        .thenReturn(List.of(constitution));
    when(constitution.getId()).thenReturn(constitutionId);
    when(constitution.getVersion()).thenReturn(3);
    when(memberships.displayNames(polityId, List.of(actorId, actorId)))
        .thenReturn(Map.of(actorId, "Ada"));

    var results = service.list(polityId, userId, 0, 50).items();

    assertThat(results)
        .extracting("entryNumber", "actorName", "constitutionVersion")
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(2, "Ada", 3),
            org.assertj.core.groups.Tuple.tuple(1, "Ada", 3));
  }

  @Test
  void emptyRecordListSkipsSecondaryLookups() {
    UUID polityId = UUID.randomUUID();
    when(records.findProjectionsByPolityIdOrderByEntryNumberDescIdAsc(
            org.mockito.ArgumentMatchers.eq(polityId),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    assertThat(service.list(polityId, UUID.randomUUID(), 0, 50).items()).isEmpty();

    verifyNoInteractions(constitutions, memberships);
  }

  @Test
  void assignsSequentialEntryNumbersForAPolity() {
    UUID polityId = UUID.randomUUID();
    OfficialRecordSequence sequence = new OfficialRecordSequence(polityId);
    when(sequences.findEntityByPolityIdForUpdate(polityId)).thenReturn(Optional.of(sequence));
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
    when(sequences.findEntityByPolityIdForUpdate(polityId)).thenReturn(Optional.empty());
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
    when(sequences.findEntityByPolityIdForUpdate(polityId))
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
            TemplateParameters.of(
                "polityName",
                "Tiny Senate",
                "setupPreset",
                "standard constitutional council republic",
                "pace",
                "fast")),
        NOW);

    ArgumentCaptor<OfficialRecordEntry> entries =
        ArgumentCaptor.forClass(OfficialRecordEntry.class);
    verify(records).save(entries.capture());
    OfficialRecordEntry entry = entries.getValue();
    assertThat(entry.getTitleKey()).isEqualTo("official_record.polity_founded.title");
    assertThat(entry.getBodyKey()).isEqualTo("official_record.polity_founded.body");
    assertThat(entry.getTitle()).isEqualTo("official_record.polity_founded.title");
    assertThat(entry.getBody()).isEqualTo("official_record.polity_founded.body");
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
            TemplateParameters.of(
                "polityName",
                "Tiny Senate",
                "setupPreset",
                "standard constitutional council republic",
                "pace",
                "fast")),
        NOW);
  }

  private OfficialRecordProjection recordProjection(
      int entryNumber, UUID constitutionId, UUID actorId) {
    OfficialRecordProjection projection = mock(OfficialRecordProjection.class);
    when(projection.getId()).thenReturn(UUID.randomUUID());
    when(projection.getEntryNumber()).thenReturn(entryNumber);
    when(projection.getConstitutionVersionId()).thenReturn(constitutionId);
    when(projection.getActorMembershipId()).thenReturn(actorId);
    when(projection.getType()).thenReturn(OfficialRecordType.POLITY_FOUNDED);
    when(projection.getTitle()).thenReturn("Founded");
    when(projection.getBody()).thenReturn("Founded body");
    when(projection.getTemplateParams()).thenReturn(Map.of());
    when(projection.getOccurredAt()).thenReturn(NOW);
    return projection;
  }
}
