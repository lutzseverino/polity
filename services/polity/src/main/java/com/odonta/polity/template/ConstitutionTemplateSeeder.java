package com.odonta.polity.template;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.InstitutionTemplateKey;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.OfficeTemplateKey;
import com.odonta.polity.model.PolityPace;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.ProcedureTemplateKey;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.ProcedureRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConstitutionTemplateSeeder {
  private static final int DEFAULT_QUORUM_NUMERATOR = 1;
  private static final int DEFAULT_QUORUM_DENOMINATOR = 2;
  private static final int DEFAULT_OFFICE_HOLDER_MINIMUM_ELECTOR_COUNT = 1;
  private static final int DEFAULT_ACTIVE_MEMBER_MINIMUM_ELECTOR_COUNT = 2;
  private static final int STANDARD_JUDICIAL_MINIMUM_ELECTOR_COUNT = 2;
  private static final int DEFAULT_OFFICE_SEAT_COUNT = 1;
  private static final int STANDARD_COUNCIL_SEAT_COUNT = 5;
  private static final int STANDARD_MAGISTRATE_SEAT_COUNT = 3;

  private final ConstitutionalPowerRepository powers;
  private final InstitutionRepository institutions;
  private final OfficeRepository offices;
  private final ProcedureRepository procedures;

  public Institution establishStandardConstitutionalCouncilRepublic(
      Jurisdiction jurisdiction, ConstitutionVersion constitution) {
    return establishStandardConstitutionalCouncilRepublic(
        jurisdiction, constitution, PolityPace.STANDARD);
  }

  public Institution establishStandardConstitutionalCouncilRepublic(
      Jurisdiction jurisdiction, ConstitutionVersion constitution, PolityPace pace) {
    Institution assembly =
        institutions.saveAndFlush(
            new Institution(
                constitution.getPolityId(),
                jurisdiction.getId(),
                constitution.getId(),
                InstitutionTemplateKey.CITIZENS_ASSEMBLY.storedName(),
                InstitutionTemplateKey.CITIZENS_ASSEMBLY,
                InstitutionKind.ASSEMBLY));
    Institution council =
        institutions.saveAndFlush(
            new Institution(
                constitution.getPolityId(),
                jurisdiction.getId(),
                constitution.getId(),
                InstitutionTemplateKey.CITIZENS_COUNCIL.storedName(),
                InstitutionTemplateKey.CITIZENS_COUNCIL,
                InstitutionKind.COUNCIL));
    Institution court =
        institutions.saveAndFlush(
            new Institution(
                constitution.getPolityId(),
                jurisdiction.getId(),
                constitution.getId(),
                InstitutionTemplateKey.MAGISTRATES_COURT.storedName(),
                InstitutionTemplateKey.MAGISTRATES_COURT,
                InstitutionKind.JUDICIARY));
    seedProcedures(constitution, assembly, council, court, pace);
    seedOffices(jurisdiction, constitution, pace);
    seedPowers(constitution);
    return assembly;
  }

  private void seedProcedures(
      ConstitutionVersion constitution,
      Institution assembly,
      Institution council,
      Institution court,
      PolityPace pace) {
    procedures.saveAllAndFlush(
        List.of(
            procedure(
                constitution,
                council,
                Procedure.ORDINARY_RESOLUTION,
                ProcedureTemplateKey.ORDINARY_RESOLUTION,
                EffectType.ADOPT_RESOLUTION,
                VotingThreshold.SIMPLE_MAJORITY_CAST,
                ProcedureElectorate.OFFICE_HOLDERS,
                Office.COUNCILOR,
                DEFAULT_OFFICE_HOLDER_MINIMUM_ELECTOR_COUNT,
                0,
                pace.ordinaryVotingPeriodHours()),
            procedure(
                constitution,
                assembly,
                Procedure.OFFICE_ELECTION,
                ProcedureTemplateKey.OFFICE_ELECTION,
                EffectType.ELECT_OFFICE,
                VotingThreshold.OFFICE_ELECTION_RESULT,
                OfficeElectionMethod.RANKED_CHOICE,
                pace.officeElectionMinimumNoticeHours(),
                pace.officeElectionVotingPeriodHours()),
            procedure(
                constitution,
                assembly,
                Procedure.SANCTION,
                ProcedureTemplateKey.SANCTION,
                EffectType.APPLY_SANCTION,
                VotingThreshold.MAJORITY_OF_ELIGIBLE,
                pace.sanctionMinimumNoticeHours(),
                pace.sanctionVotingPeriodHours()),
            procedure(
                constitution,
                court,
                Procedure.APPEAL,
                ProcedureTemplateKey.APPEAL,
                EffectType.GRANT_APPEAL,
                VotingThreshold.SIMPLE_MAJORITY_CAST,
                ProcedureElectorate.OFFICE_HOLDERS,
                Office.MAGISTRATE,
                STANDARD_JUDICIAL_MINIMUM_ELECTOR_COUNT,
                0,
                pace.ordinaryVotingPeriodHours()),
            procedure(
                constitution,
                court,
                Procedure.OFFICE_TERM_REVIEW,
                ProcedureTemplateKey.OFFICE_TERM_REVIEW,
                EffectType.VACATE_OFFICE_TERM,
                VotingThreshold.SIMPLE_MAJORITY_CAST,
                ProcedureElectorate.OFFICE_HOLDERS,
                Office.MAGISTRATE,
                STANDARD_JUDICIAL_MINIMUM_ELECTOR_COUNT,
                0,
                pace.ordinaryVotingPeriodHours()),
            procedure(
                constitution,
                court,
                Procedure.CONSTITUTIONAL_REVIEW,
                ProcedureTemplateKey.CONSTITUTIONAL_REVIEW,
                EffectType.VOID_OFFICIAL_ACT,
                VotingThreshold.SIMPLE_MAJORITY_CAST,
                ProcedureElectorate.OFFICE_HOLDERS,
                Office.MAGISTRATE,
                STANDARD_JUDICIAL_MINIMUM_ELECTOR_COUNT,
                0,
                pace.ordinaryVotingPeriodHours()),
            procedure(
                constitution,
                assembly,
                Procedure.CONSTITUTION_AMENDMENT,
                ProcedureTemplateKey.CONSTITUTION_AMENDMENT,
                EffectType.AMEND_CONSTITUTION,
                VotingThreshold.TWO_THIRDS_ELIGIBLE,
                pace.constitutionalAmendmentMinimumNoticeHours(),
                pace.constitutionalAmendmentVotingPeriodHours()),
            procedure(
                constitution,
                assembly,
                Procedure.DISBANDMENT,
                ProcedureTemplateKey.DISBANDMENT,
                EffectType.DISBAND_POLITY,
                VotingThreshold.TWO_THIRDS_ELIGIBLE,
                pace.constitutionalAmendmentMinimumNoticeHours(),
                pace.constitutionalAmendmentVotingPeriodHours())));
  }

  private Procedure procedure(
      ConstitutionVersion constitution,
      Institution institution,
      String code,
      ProcedureTemplateKey templateKey,
      EffectType effectType,
      VotingThreshold threshold,
      OfficeElectionMethod officeElectionMethod,
      int minimumNoticeHours,
      int votingPeriodHours) {
    return procedure(
        constitution,
        institution,
        code,
        templateKey,
        effectType,
        threshold,
        ProcedureElectorate.ACTIVE_MEMBERS,
        null,
        DEFAULT_ACTIVE_MEMBER_MINIMUM_ELECTOR_COUNT,
        minimumNoticeHours,
        votingPeriodHours,
        officeElectionMethod);
  }

  private Procedure procedure(
      ConstitutionVersion constitution,
      Institution institution,
      String code,
      ProcedureTemplateKey templateKey,
      EffectType effectType,
      VotingThreshold threshold,
      int minimumNoticeHours,
      int votingPeriodHours) {
    return procedure(
        constitution,
        institution,
        code,
        templateKey,
        effectType,
        threshold,
        ProcedureElectorate.ACTIVE_MEMBERS,
        null,
        DEFAULT_ACTIVE_MEMBER_MINIMUM_ELECTOR_COUNT,
        minimumNoticeHours,
        votingPeriodHours,
        null);
  }

  private Procedure procedure(
      ConstitutionVersion constitution,
      Institution institution,
      String code,
      ProcedureTemplateKey templateKey,
      EffectType effectType,
      VotingThreshold threshold,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      int minimumElectorCount,
      int minimumNoticeHours,
      int votingPeriodHours) {
    return procedure(
        constitution,
        institution,
        code,
        templateKey,
        effectType,
        threshold,
        electorate,
        electorateOfficeCode,
        minimumElectorCount,
        minimumNoticeHours,
        votingPeriodHours,
        null);
  }

  private Procedure procedure(
      ConstitutionVersion constitution,
      Institution institution,
      String code,
      ProcedureTemplateKey templateKey,
      EffectType effectType,
      VotingThreshold threshold,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      int minimumElectorCount,
      int minimumNoticeHours,
      int votingPeriodHours,
      OfficeElectionMethod officeElectionMethod) {
    return new Procedure(
        constitution.getPolityId(),
        constitution.getId(),
        institution.getId(),
        code,
        templateKey.storedName(),
        templateKey,
        DEFAULT_QUORUM_NUMERATOR,
        DEFAULT_QUORUM_DENOMINATOR,
        threshold,
        electorate,
        electorateOfficeCode,
        minimumElectorCount,
        minimumNoticeHours,
        votingPeriodHours,
        effectType,
        officeElectionMethod);
  }

  private void seedOffices(
      Jurisdiction jurisdiction, ConstitutionVersion constitution, PolityPace pace) {
    offices.saveAllAndFlush(
        List.of(
            new Office(
                constitution.getPolityId(),
                constitution.getId(),
                jurisdiction.getId(),
                Office.COUNCILOR,
                OfficeTemplateKey.COUNCILOR.storedName(),
                OfficeTemplateKey.COUNCILOR.storedDescription(),
                OfficeTemplateKey.COUNCILOR,
                pace.starterOfficeTermDays(),
                STANDARD_COUNCIL_SEAT_COUNT),
            new Office(
                constitution.getPolityId(),
                constitution.getId(),
                jurisdiction.getId(),
                Office.STEWARD,
                OfficeTemplateKey.STEWARD.storedName(),
                OfficeTemplateKey.STEWARD.storedDescription(),
                OfficeTemplateKey.STEWARD,
                pace.starterOfficeTermDays(),
                DEFAULT_OFFICE_SEAT_COUNT),
            new Office(
                constitution.getPolityId(),
                constitution.getId(),
                jurisdiction.getId(),
                Office.MAGISTRATE,
                OfficeTemplateKey.MAGISTRATE.storedName(),
                OfficeTemplateKey.MAGISTRATE.storedDescription(),
                OfficeTemplateKey.MAGISTRATE,
                pace.starterOfficeTermDays(),
                STANDARD_MAGISTRATE_SEAT_COUNT),
            new Office(
                constitution.getPolityId(),
                constitution.getId(),
                jurisdiction.getId(),
                Office.TRIBUNE,
                OfficeTemplateKey.TRIBUNE.storedName(),
                OfficeTemplateKey.TRIBUNE.storedDescription(),
                OfficeTemplateKey.TRIBUNE,
                pace.starterOfficeTermDays(),
                DEFAULT_OFFICE_SEAT_COUNT)));
  }

  private void seedPowers(ConstitutionVersion constitution) {
    powers.saveAllAndFlush(
        List.of(
            power(constitution, PowerCode.INTRODUCE_MOTION),
            power(constitution, PowerCode.INTRODUCE_OFFICE_ELECTION),
            officePower(constitution, PowerCode.INTRODUCE_SANCTION, Office.TRIBUNE),
            power(constitution, PowerCode.INTRODUCE_APPEAL),
            power(constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW),
            power(constitution, PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW),
            power(constitution, PowerCode.INTRODUCE_AMENDMENT),
            power(constitution, PowerCode.INTRODUCE_DISBANDMENT),
            officePower(constitution, PowerCode.ADMIT_MEMBER, Office.STEWARD),
            power(constitution, PowerCode.REQUEST_CERTIFICATION)));
  }

  private ConstitutionalPower power(ConstitutionVersion constitution, PowerCode code) {
    return ConstitutionalPower.defaultNamed(
        constitution.getPolityId(), constitution.getId(), code, PowerHolderScope.ACTIVE_MEMBER);
  }

  private ConstitutionalPower officePower(
      ConstitutionVersion constitution, PowerCode code, String officeCode) {
    return ConstitutionalPower.defaultNamedForOffice(
        constitution.getPolityId(), constitution.getId(), code, officeCode);
  }
}
