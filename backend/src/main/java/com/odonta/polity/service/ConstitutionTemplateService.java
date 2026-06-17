package com.odonta.polity.service;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.ProcedureRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConstitutionTemplateService {
  static final String STARTER_CONSTITUTION =
      "All active citizens may propose resolutions, office assignments, sanctions, appeals, and "
          + "constitutional amendments. The Steward admits citizens and certifies proceedings after "
          + "the governing voting window closes. Ordinary proceedings require participation by at "
          + "least half of the eligible electorate. Sanctions and office assignments require a "
          + "majority of eligible citizens, and constitutional amendments require two thirds of "
          + "eligible citizens.";

  private final ConstitutionalPowerRepository powers;
  private final InstitutionRepository institutions;
  private final OfficeRepository offices;
  private final ProcedureRepository procedures;

  public Institution establishStarterRepublic(
      Jurisdiction jurisdiction, ConstitutionVersion constitution) {
    Institution institution =
        institutions.saveAndFlush(
            new Institution(
                constitution.getPolityId(),
                jurisdiction.getId(),
                constitution.getId(),
                "Citizens' Assembly",
                InstitutionKind.ASSEMBLY));
    seedProcedures(constitution, institution);
    seedOffices(jurisdiction, constitution);
    seedPowers(constitution);
    return institution;
  }

  private void seedProcedures(ConstitutionVersion constitution, Institution institution) {
    procedures.saveAllAndFlush(
        List.of(
            procedure(
                constitution,
                institution,
                Procedure.ORDINARY_RESOLUTION,
                "Ordinary resolution",
                EffectType.ADOPT_RESOLUTION,
                1,
                2,
                VotingThreshold.SIMPLE_MAJORITY_CAST,
                0,
                24),
            procedure(
                constitution,
                institution,
                Procedure.OFFICE_ASSIGNMENT,
                "Office assignment",
                EffectType.ASSIGN_OFFICE,
                1,
                2,
                VotingThreshold.MAJORITY_OF_ELIGIBLE,
                12,
                24),
            procedure(
                constitution,
                institution,
                Procedure.SANCTION,
                "Sanction",
                EffectType.APPLY_SANCTION,
                1,
                2,
                VotingThreshold.MAJORITY_OF_ELIGIBLE,
                12,
                24),
            procedure(
                constitution,
                institution,
                Procedure.APPEAL,
                "Appeal",
                EffectType.GRANT_APPEAL,
                1,
                2,
                VotingThreshold.SIMPLE_MAJORITY_CAST,
                0,
                24),
            procedure(
                constitution,
                institution,
                Procedure.CONSTITUTION_AMENDMENT,
                "Constitutional amendment",
                EffectType.AMEND_CONSTITUTION,
                1,
                2,
                VotingThreshold.TWO_THIRDS_ELIGIBLE,
                24,
                72)));
  }

  private Procedure procedure(
      ConstitutionVersion constitution,
      Institution institution,
      String code,
      String name,
      EffectType effectType,
      int quorumNumerator,
      int quorumDenominator,
      VotingThreshold threshold,
      int minimumNoticeHours,
      int votingPeriodHours) {
    return new Procedure(
        constitution.getPolityId(),
        constitution.getId(),
        institution.getId(),
        code,
        name,
        quorumNumerator,
        quorumDenominator,
        threshold,
        minimumNoticeHours,
        votingPeriodHours,
        effectType);
  }

  private void seedOffices(Jurisdiction jurisdiction, ConstitutionVersion constitution) {
    offices.saveAndFlush(
        new Office(
            constitution.getPolityId(),
            constitution.getId(),
            jurisdiction.getId(),
            Office.STEWARD,
            "Steward",
            "Coordinates official proceedings and can hold constitution-defined powers.",
            90));
  }

  private void seedPowers(ConstitutionVersion constitution) {
    powers.saveAllAndFlush(
        List.of(
            power(constitution, PowerCode.INTRODUCE_MOTION, "Introduce resolutions"),
            power(
                constitution, PowerCode.INTRODUCE_OFFICE_ASSIGNMENT, "Propose office assignments"),
            power(constitution, PowerCode.INTRODUCE_SANCTION, "Propose sanctions"),
            power(constitution, PowerCode.INTRODUCE_APPEAL, "Propose appeals"),
            power(constitution, PowerCode.INTRODUCE_AMENDMENT, "Propose constitutional amendments"),
            officePower(constitution, PowerCode.ADMIT_MEMBER, "Admit citizens", Office.STEWARD),
            officePower(
                constitution,
                PowerCode.REQUEST_CERTIFICATION,
                "Request certification",
                Office.STEWARD)));
  }

  private ConstitutionalPower power(ConstitutionVersion constitution, PowerCode code, String name) {
    return new ConstitutionalPower(
        constitution.getPolityId(),
        constitution.getId(),
        code,
        name,
        PowerHolderScope.ACTIVE_MEMBER);
  }

  private ConstitutionalPower officePower(
      ConstitutionVersion constitution, PowerCode code, String name, String officeCode) {
    return new ConstitutionalPower(
        constitution.getPolityId(), constitution.getId(), code, name, officeCode);
  }
}
