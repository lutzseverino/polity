package com.odonta.polity.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.api.model.ActionAvailabilityResponse;
import com.odonta.polity.model.CertificationModality;
import com.odonta.polity.model.CertificationOutcomeReason;
import com.odonta.polity.model.GovernmentReadinessDiagnostic;
import com.odonta.polity.model.GovernmentReadinessStatus;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.repository.CertificationProjection;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficialRecordProjection;
import com.odonta.polity.result.ActionAvailabilityResult;
import com.odonta.polity.result.ActionUnavailableReason;
import com.odonta.polity.result.CertificationResult;
import com.odonta.polity.result.GovernmentReadinessResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

class MapperTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-16T12:00:00Z");

  private final CertificationApplicationMapper certifications =
      Mappers.getMapper(CertificationApplicationMapper.class);
  private final OfficeApplicationMapper offices = Mappers.getMapper(OfficeApplicationMapper.class);
  private final OfficeTermApplicationMapper officeTerms =
      Mappers.getMapper(OfficeTermApplicationMapper.class);
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
  void mapsOfficeTermPersistedOfficeIdentity() {
    UUID officeId = UUID.randomUUID();
    OfficeTermProjection projection = mock(OfficeTermProjection.class);
    when(projection.getId()).thenReturn(UUID.randomUUID());
    when(projection.getOfficeId()).thenReturn(officeId);
    when(projection.getMembershipId()).thenReturn(UUID.randomUUID());
    when(projection.getStatus()).thenReturn(OfficeTermStatus.ACTIVE);
    when(projection.getStartedAt()).thenReturn(NOW.minusDays(1));
    when(projection.getEndsAt()).thenReturn(NOW.plusDays(1));

    var result =
        officeTerms.toResult(
            projection, "Steward", "office.steward.name", "Ada", OfficeTermStatus.ACTIVE);

    assertThat(result.officeId()).isEqualTo(officeId);
  }

  @Test
  void mapsCertificationResult() {
    CertificationProjection certification = mock(CertificationProjection.class);
    when(certification.getModality()).thenReturn(CertificationModality.YES_NO);
    when(certification.getEligibleCount()).thenReturn(3);
    when(certification.getYesCount()).thenReturn(2);
    when(certification.getNoCount()).thenReturn(1);
    when(certification.getAbstainCount()).thenReturn(0);
    when(certification.getQuorumRequired()).thenReturn(2);
    when(certification.isQuorumMet()).thenReturn(true);
    when(certification.isThresholdMet()).thenReturn(true);
    when(certification.isPassed()).thenReturn(true);
    when(certification.getOutcomeReason()).thenReturn(CertificationOutcomeReason.PASSED);
    when(certification.getCertifiedAt()).thenReturn(NOW);

    var result = certifications.toResult(certification);

    assertThat(result.modality()).isEqualTo(CertificationModality.YES_NO);
    assertThat(result.eligibleCount()).isEqualTo(3);
    assertThat(result.yesCount()).isEqualTo(2);
    assertThat(result.quorumRequired()).isEqualTo(2);
    assertThat(result.passed()).isTrue();
    assertThat(result.outcomeReason()).isEqualTo(CertificationOutcomeReason.PASSED);
    assertThat(result.certifiedAt()).isEqualTo(NOW);
  }

  @Test
  void mapsDurableCertificationEvidenceToTransport() {
    CertificationTransportMapper mapper = Mappers.getMapper(CertificationTransportMapper.class);
    CertificationResult result =
        new CertificationResult(
            CertificationModality.OFFICE_ELECTION,
            7,
            null,
            null,
            null,
            6,
            true,
            2,
            4,
            true,
            true,
            true,
            CertificationOutcomeReason.PASSED,
            NOW);

    var response = mapper.toResponse(result);

    assertThat(response.getModality())
        .isEqualTo(com.odonta.polity.api.model.CertificationModality.OFFICE_ELECTION);
    assertThat(response.getEligibleCount()).isEqualTo(7);
    assertThat(response.getElectionParticipationCount()).isEqualTo(6);
    assertThat(response.getElectionDecisive()).isTrue();
    assertThat(response.getElectionWinnerCount()).isEqualTo(2);
    assertThat(response.getYesCount()).isNull();
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

  @Test
  void mapsEveryOfficialRecordTypeThroughItsOwnedTransportMapper() {
    OfficialRecordTypeTransportMapper mapper =
        Mappers.getMapper(OfficialRecordTypeTransportMapper.class);

    assertThat(OfficialRecordType.values())
        .allSatisfy(
            type -> assertThat(mapper.toTransport(type).getValue()).isEqualTo(type.wireValue()));
  }

  @Test
  void mapsActionAvailabilityWithLocalizedReasonMessage() {
    try {
      LocaleContextHolder.setLocale(Locale.ENGLISH);
      StaticMessageSource messages = new StaticMessageSource();
      messages.addMessage(
          "api_error.polity_disbanded", Locale.ENGLISH, "This polity has been disbanded.");
      TransportTextResolver text =
          new TransportTextResolver(new ConstitutionChangeTextResolver(messages), messages);
      ActionAvailabilityTransportMapper mapper =
          Mappers.getMapper(ActionAvailabilityTransportMapper.class);
      ReflectionTestUtils.setField(
          mapper, "actionAvailabilityTransportText", new ActionAvailabilityTransportText(text));

      ActionAvailabilityResponse response =
          mapper.toResponse(
              ActionAvailabilityResult.blocked(ActionUnavailableReason.POLITY_DISBANDED));

      assertThat(response.getAvailable()).isFalse();
      assertThat(response.getReason())
          .isEqualTo(com.odonta.polity.api.model.ActionUnavailableReason.POLITY_DISBANDED);
      assertThat(response.getReasonMessage()).isEqualTo("This polity has been disbanded.");
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test
  void mapsEveryActionUnavailableReasonToTheGeneratedContractVocabulary() {
    ActionAvailabilityTransportMapper mapper =
        Mappers.getMapper(ActionAvailabilityTransportMapper.class);

    assertThat(ActionUnavailableReason.values())
        .allSatisfy(
            reason ->
                assertThat(mapper.toTransport(reason).getValue()).isEqualTo(reason.wireValue()));
  }

  @Test
  void mapsGovernmentReadinessWithLocalizedDiagnostics() {
    try {
      LocaleContextHolder.setLocale(Locale.ENGLISH);
      StaticMessageSource messages = new StaticMessageSource();
      messages.addMessage("government_readiness.status.provisional", Locale.ENGLISH, "Provisional");
      messages.addMessage(
          "government_readiness.diagnostic.needs_more_standing_members",
          Locale.ENGLISH,
          "More citizens are needed.");
      TransportTextResolver text =
          new TransportTextResolver(new ConstitutionChangeTextResolver(messages), messages);
      GovernmentReadinessTransportMapper mapper =
          Mappers.getMapper(GovernmentReadinessTransportMapper.class);
      ReflectionTestUtils.setField(
          mapper, "governmentAssessmentTransportText", new GovernmentAssessmentTransportText(text));

      var response =
          mapper.toResponse(
              new GovernmentReadinessResult(
                  GovernmentReadinessStatus.PROVISIONAL,
                  List.of(GovernmentReadinessDiagnostic.NEEDS_MORE_STANDING_MEMBERS)));

      assertThat(response.getStatus())
          .isEqualTo(com.odonta.polity.api.model.GovernmentReadinessStatus.PROVISIONAL);
      assertThat(response.getStatusMessage()).isEqualTo("Provisional");
      assertThat(response.getDiagnostics())
          .singleElement()
          .satisfies(
              diagnostic -> {
                assertThat(diagnostic.getCode())
                    .isEqualTo(
                        com.odonta.polity.api.model.GovernmentReadinessDiagnostic
                            .NEEDS_MORE_STANDING_MEMBERS);
                assertThat(diagnostic.getMessage()).isEqualTo("More citizens are needed.");
              });
    } finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }
}
