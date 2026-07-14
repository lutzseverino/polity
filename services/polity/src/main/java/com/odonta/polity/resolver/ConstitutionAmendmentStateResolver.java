package com.odonta.polity.resolver;

import com.odonta.polity.model.ConstitutionAmendmentPowerState;
import com.odonta.polity.model.ConstitutionAmendmentProcedureState;
import com.odonta.polity.model.ConstitutionAmendmentState;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConstitutionAmendmentStateResolver {
  private final ConstitutionalPowerRepository powers;
  private final InstitutionRepository institutions;
  private final JurisdictionRepository jurisdictions;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final ProcedureRepository procedures;

  public ConstitutionAmendmentState resolve(ConstitutionVersion constitution, OffsetDateTime now) {
    UUID polityId = constitution.getPolityId();
    Map<UUID, com.odonta.polity.model.InstitutionKind> institutionKinds =
        institutions.findEntitiesByConstitutionVersionId(constitution.getId()).stream()
            .collect(Collectors.toMap(Institution::getId, Institution::getKind));
    java.util.Set<String> officeCodes =
        offices.findEntitiesByConstitutionVersionIdOrderByName(constitution.getId()).stream()
            .map(Office::getCode)
            .collect(Collectors.toSet());
    Map<String, ConstitutionAmendmentProcedureState> currentProcedures =
        procedures.findEntitiesByConstitutionVersionId(constitution.getId()).stream()
            .collect(
                Collectors.toMap(
                    Procedure::getCode,
                    procedure ->
                        new ConstitutionAmendmentProcedureState(
                            procedure.getInstitutionId(),
                            procedure.getEffectType(),
                            procedure.getThreshold(),
                            procedure.getOfficeElectionMethod(),
                            procedure.getElectorate(),
                            procedure.getElectorateOfficeCode())));
    Map<PowerCode, ConstitutionAmendmentPowerState> currentPowers =
        powers.findEntitiesByConstitutionVersionId(constitution.getId()).stream()
            .collect(
                Collectors.toMap(
                    ConstitutionalPower::getCode,
                    power ->
                        new ConstitutionAmendmentPowerState(
                            power.getHolderScope(), power.getHolderOfficeCode())));
    Map<String, Long> activeOfficeTermCounts =
        officeTerms.findEntitiesByPolityIdAndStatus(polityId, OfficeTermStatus.ACTIVE).stream()
            .filter(term -> term.getEndsAt().isAfter(now))
            .collect(Collectors.groupingBy(term -> term.getOfficeCode(), Collectors.counting()));
    return new ConstitutionAmendmentState(
        institutionKinds,
        officeCodes,
        currentProcedures,
        currentPowers,
        jurisdictions.findEntitiesByPolityId(polityId).stream()
            .map(Jurisdiction::getId)
            .collect(Collectors.toSet()),
        activeOfficeTermCounts);
  }
}
