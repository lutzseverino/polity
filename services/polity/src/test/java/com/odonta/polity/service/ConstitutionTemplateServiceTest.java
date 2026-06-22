package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.ConstitutionalPowerTemplateKey;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionTemplateKey;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTemplateKey;
import com.odonta.polity.model.PolityPace;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.ProcedureTemplateKey;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.ProcedureRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ConstitutionTemplateServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-17T10:00:00Z");

  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final InstitutionRepository institutions = mock(InstitutionRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final ConstitutionTemplateService service =
      new ConstitutionTemplateService(powers, institutions, offices, procedures);

  @Test
  @SuppressWarnings("unchecked")
  void standardRepublicPaceControlsBootstrapTermAndVotingWindows() {
    UUID polityId = UUID.randomUUID();
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", "Body", NOW);
    ReflectionTestUtils.setField(constitution, "id", UUID.randomUUID());
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    service.establishStarterRepublic(jurisdiction, constitution, PolityPace.STANDARD);

    ArgumentCaptor<Institution> institutionCaptor = ArgumentCaptor.forClass(Institution.class);
    verify(institutions).saveAndFlush(institutionCaptor.capture());
    assertThat(institutionCaptor.getValue().getNameKey())
        .isEqualTo(InstitutionTemplateKey.CITIZENS_ASSEMBLY.nameKey());

    ArgumentCaptor<List<Office>> officeCaptor = ArgumentCaptor.forClass(List.class);
    verify(offices).saveAllAndFlush(officeCaptor.capture());
    assertThat(officeCaptor.getValue())
        .extracting(Office::getCode)
        .containsExactly(Office.STEWARD, Office.MAGISTRATE, Office.TRIBUNE);
    assertThat(officeCaptor.getValue())
        .filteredOn(office -> office.getCode().equals(Office.STEWARD))
        .singleElement()
        .extracting(Office::getTermLengthDays)
        .isEqualTo(14);

    ArgumentCaptor<List<Procedure>> procedureCaptor = ArgumentCaptor.forClass(List.class);
    verify(procedures).saveAllAndFlush(procedureCaptor.capture());
    assertThat(procedureCaptor.getValue())
        .filteredOn(procedure -> procedure.getCode().equals(Procedure.ORDINARY_RESOLUTION))
        .singleElement()
        .extracting(Procedure::getVotingPeriodHours)
        .isEqualTo(48);
    assertThat(procedureCaptor.getValue())
        .filteredOn(procedure -> procedure.getCode().equals(Procedure.CONSTITUTION_AMENDMENT))
        .singleElement()
        .satisfies(
            procedure -> {
              assertThat(procedure.getMinimumNoticeHours()).isEqualTo(24);
              assertThat(procedure.getVotingPeriodHours()).isEqualTo(120);
            });
    assertThat(procedureCaptor.getValue())
        .filteredOn(procedure -> procedure.getCode().equals(Procedure.DISBANDMENT))
        .singleElement()
        .satisfies(
            procedure -> {
              assertThat(procedure.getMinimumNoticeHours()).isEqualTo(24);
              assertThat(procedure.getVotingPeriodHours()).isEqualTo(120);
            });
  }

  @Test
  @SuppressWarnings("unchecked")
  void certificationPowerBelongsToActiveCitizens() {
    UUID polityId = UUID.randomUUID();
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", "Body", NOW);
    ReflectionTestUtils.setField(constitution, "id", UUID.randomUUID());
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    service.establishStarterRepublic(jurisdiction, constitution, PolityPace.STANDARD);

    ArgumentCaptor<List<ConstitutionalPower>> powerCaptor = ArgumentCaptor.forClass(List.class);
    verify(powers).saveAllAndFlush(powerCaptor.capture());
    assertThat(powerCaptor.getValue())
        .filteredOn(power -> power.getCode() == PowerCode.INTRODUCE_OFFICE_ELECTION)
        .singleElement()
        .satisfies(
            power -> {
              assertThat(power.getHolderScope()).isEqualTo(PowerHolderScope.ACTIVE_MEMBER);
              assertThat(power.getHolderOfficeCode()).isNull();
            });
    assertThat(powerCaptor.getValue())
        .filteredOn(power -> power.getCode() == PowerCode.INTRODUCE_SANCTION)
        .singleElement()
        .satisfies(
            power -> {
              assertThat(power.getHolderScope()).isEqualTo(PowerHolderScope.OFFICE);
              assertThat(power.getHolderOfficeCode()).isEqualTo(Office.TRIBUNE);
            });
    assertThat(powerCaptor.getValue())
        .filteredOn(power -> power.getCode() == PowerCode.REQUEST_CERTIFICATION)
        .singleElement()
        .satisfies(
            power -> {
              assertThat(power.getHolderScope()).isEqualTo(PowerHolderScope.ACTIVE_MEMBER);
              assertThat(power.getHolderOfficeCode()).isNull();
            });
    assertThat(powerCaptor.getValue())
        .filteredOn(power -> power.getCode() == PowerCode.INTRODUCE_DISBANDMENT)
        .singleElement()
        .satisfies(
            power -> {
              assertThat(power.getHolderScope()).isEqualTo(PowerHolderScope.ACTIVE_MEMBER);
              assertThat(power.getHolderOfficeCode()).isNull();
            });
  }

  @Test
  @SuppressWarnings("unchecked")
  void standardRepublicSeedsTranslationKeysForSystemConstitutionalContent() {
    UUID polityId = UUID.randomUUID();
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", "Body", NOW);
    ReflectionTestUtils.setField(constitution, "id", UUID.randomUUID());
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    service.establishStarterRepublic(jurisdiction, constitution, PolityPace.STANDARD);

    ArgumentCaptor<Institution> institutionCaptor = ArgumentCaptor.forClass(Institution.class);
    verify(institutions).saveAndFlush(institutionCaptor.capture());
    assertThat(institutionCaptor.getValue().getNameKey())
        .isEqualTo(InstitutionTemplateKey.CITIZENS_ASSEMBLY.nameKey());

    ArgumentCaptor<List<Office>> officeCaptor = ArgumentCaptor.forClass(List.class);
    verify(offices).saveAllAndFlush(officeCaptor.capture());
    assertThat(officeCaptor.getValue())
        .extracting(Office::getCode, Office::getNameKey, Office::getDescriptionKey)
        .containsExactly(
            tuple(
                Office.STEWARD,
                OfficeTemplateKey.STEWARD.nameKey(),
                OfficeTemplateKey.STEWARD.descriptionKey()),
            tuple(
                Office.MAGISTRATE,
                OfficeTemplateKey.MAGISTRATE.nameKey(),
                OfficeTemplateKey.MAGISTRATE.descriptionKey()),
            tuple(
                Office.TRIBUNE,
                OfficeTemplateKey.TRIBUNE.nameKey(),
                OfficeTemplateKey.TRIBUNE.descriptionKey()));

    ArgumentCaptor<List<Procedure>> procedureCaptor = ArgumentCaptor.forClass(List.class);
    verify(procedures).saveAllAndFlush(procedureCaptor.capture());
    assertThat(procedureCaptor.getValue())
        .extracting(Procedure::getCode, Procedure::getNameKey)
        .containsExactly(
            tuple(
                Procedure.ORDINARY_RESOLUTION, ProcedureTemplateKey.ORDINARY_RESOLUTION.nameKey()),
            tuple(Procedure.OFFICE_ELECTION, ProcedureTemplateKey.OFFICE_ELECTION.nameKey()),
            tuple(Procedure.SANCTION, ProcedureTemplateKey.SANCTION.nameKey()),
            tuple(Procedure.APPEAL, ProcedureTemplateKey.APPEAL.nameKey()),
            tuple(
                Procedure.CONSTITUTION_AMENDMENT,
                ProcedureTemplateKey.CONSTITUTION_AMENDMENT.nameKey()),
            tuple(Procedure.DISBANDMENT, ProcedureTemplateKey.DISBANDMENT.nameKey()));
    assertThat(procedureCaptor.getValue())
        .filteredOn(procedure -> procedure.getCode().equals(Procedure.APPEAL))
        .singleElement()
        .satisfies(
            procedure -> {
              assertThat(procedure.getElectorate()).isEqualTo(ProcedureElectorate.OFFICE_HOLDERS);
              assertThat(procedure.getElectorateOfficeCode()).isEqualTo(Office.MAGISTRATE);
            });

    ArgumentCaptor<List<ConstitutionalPower>> powerCaptor = ArgumentCaptor.forClass(List.class);
    verify(powers).saveAllAndFlush(powerCaptor.capture());
    assertThat(powerCaptor.getValue())
        .extracting(ConstitutionalPower::getCode, ConstitutionalPower::getNameKey)
        .containsExactly(
            tuple(
                PowerCode.INTRODUCE_MOTION,
                ConstitutionalPowerTemplateKey.INTRODUCE_MOTION.nameKey()),
            tuple(
                PowerCode.INTRODUCE_OFFICE_ELECTION,
                ConstitutionalPowerTemplateKey.INTRODUCE_OFFICE_ELECTION.nameKey()),
            tuple(
                PowerCode.INTRODUCE_SANCTION,
                ConstitutionalPowerTemplateKey.INTRODUCE_SANCTION.nameKey()),
            tuple(
                PowerCode.INTRODUCE_APPEAL,
                ConstitutionalPowerTemplateKey.INTRODUCE_APPEAL.nameKey()),
            tuple(
                PowerCode.INTRODUCE_AMENDMENT,
                ConstitutionalPowerTemplateKey.INTRODUCE_AMENDMENT.nameKey()),
            tuple(
                PowerCode.INTRODUCE_DISBANDMENT,
                ConstitutionalPowerTemplateKey.INTRODUCE_DISBANDMENT.nameKey()),
            tuple(PowerCode.ADMIT_MEMBER, ConstitutionalPowerTemplateKey.ADMIT_MEMBER.nameKey()),
            tuple(
                PowerCode.REQUEST_CERTIFICATION,
                ConstitutionalPowerTemplateKey.REQUEST_CERTIFICATION.nameKey()));
  }

  private <T> T withId(T entity) {
    if (ReflectionTestUtils.getField(entity, "id") == null) {
      ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
    return entity;
  }
}
