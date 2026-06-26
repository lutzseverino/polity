package com.odonta.polity.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.Certification;
import com.odonta.polity.model.CertificationOutcomeReason;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.VotingOutcomeReason;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficialRecordProjection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class MapperTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-16T12:00:00Z");

  private final MotionApplicationMapper motions = Mappers.getMapper(MotionApplicationMapper.class);
  private final OfficeApplicationMapper offices = Mappers.getMapper(OfficeApplicationMapper.class);
  private final OfficialRecordApplicationMapper officialRecords =
      Mappers.getMapper(OfficialRecordApplicationMapper.class);
  private final MembershipApplicationMapper memberships =
      Mappers.getMapper(MembershipApplicationMapper.class);

  @Test
  void mapsMembershipProjectionOwnedFields() {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    MembershipProjection projection = mock(MembershipProjection.class);
    when(projection.getId()).thenReturn(id);
    when(projection.getUserId()).thenReturn(userId);
    when(projection.getDisplayName()).thenReturn("Ada");
    when(projection.getEmail()).thenReturn("ada@example.com");
    when(projection.getStatus()).thenReturn(MembershipStatus.ACTIVE);
    when(projection.getAdmittedAt()).thenReturn(NOW);

    var result = memberships.toResult(projection);

    assertThat(result.id()).isEqualTo(id);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.name()).isEqualTo("Ada");
    assertThat(result.email()).isEqualTo("ada@example.com");
    assertThat(result.status()).isEqualTo(MembershipStatus.ACTIVE);
    assertThat(result.admittedAt()).isEqualTo(NOW);
  }

  @Test
  void mapsOfficeProjectionOwnedFields() {
    UUID id = UUID.randomUUID();
    OfficeProjection projection = mock(OfficeProjection.class);
    when(projection.getId()).thenReturn(id);
    when(projection.getCode()).thenReturn("STEWARD");
    when(projection.getName()).thenReturn("Steward");
    when(projection.getDescription()).thenReturn("Keeps the lights on.");
    when(projection.getNameKey()).thenReturn("office.steward.name");
    when(projection.getDescriptionKey()).thenReturn("office.steward.description");
    when(projection.getTermLengthDays()).thenReturn(14);
    when(projection.getSeatCount()).thenReturn(3);

    var results = offices.toResults(List.of(projection));

    assertThat(results)
        .singleElement()
        .satisfies(
            result -> {
              assertThat(result.id()).isEqualTo(id);
              assertThat(result.code()).isEqualTo("STEWARD");
              assertThat(result.name()).isEqualTo("Steward");
              assertThat(result.description()).isEqualTo("Keeps the lights on.");
              assertThat(result.nameKey()).isEqualTo("office.steward.name");
              assertThat(result.descriptionKey()).isEqualTo("office.steward.description");
              assertThat(result.termLengthDays()).isEqualTo(14);
              assertThat(result.seatCount()).isEqualTo(3);
            });
  }

  @Test
  void mapsCertificationResult() {
    Certification certification =
        new Certification(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new VotingResult(3, 2, 1, 0, 2, true, true, true, VotingOutcomeReason.PASSED),
            NOW);

    var result = motions.toResult(certification);

    assertThat(result.passed()).isTrue();
    assertThat(result.outcomeReason()).isEqualTo(CertificationOutcomeReason.PASSED);
    assertThat(result.certifiedAt()).isEqualTo(NOW);
  }

  @Test
  void mapsOfficialRecordProjectionWithResolvedOwnerFields() {
    UUID id = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    OfficialRecordProjection projection = mock(OfficialRecordProjection.class);
    when(projection.getId()).thenReturn(id);
    when(projection.getEntryNumber()).thenReturn(7);
    when(projection.getType()).thenReturn(OfficialRecordType.MEMBER_ADMITTED);
    when(projection.getTitle()).thenReturn("Member admitted");
    when(projection.getBody()).thenReturn("Ada joined.");
    when(projection.getTitleKey()).thenReturn("official_record.member_admitted.title");
    when(projection.getBodyKey()).thenReturn("official_record.member_admitted.body");
    when(projection.getTemplateParams()).thenReturn(Map.of("memberName", "Ada"));
    when(projection.getSourceId()).thenReturn(sourceId);
    when(projection.getOccurredAt()).thenReturn(NOW);

    var result = officialRecords.toResult(projection, "Ada", 3);

    assertThat(result.id()).isEqualTo(id);
    assertThat(result.entryNumber()).isEqualTo(7);
    assertThat(result.type()).isEqualTo(OfficialRecordType.MEMBER_ADMITTED);
    assertThat(result.actorName()).isEqualTo("Ada");
    assertThat(result.constitutionVersion()).isEqualTo(3);
    assertThat(result.sourceId()).isEqualTo(sourceId);
    assertThat(result.occurredAt()).isEqualTo(NOW);
  }
}
