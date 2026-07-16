package com.odonta.polity.resolver;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalHealthDiagnostic;
import com.odonta.polity.model.ConstitutionalHealthStatus;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.GovernmentReadinessDiagnostic;
import com.odonta.polity.model.GovernmentReadinessStatus;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.result.ActionAvailabilityResult;
import com.odonta.polity.result.ActionUnavailableReason;
import com.odonta.polity.result.ConstitutionalHealthResult;
import com.odonta.polity.result.GovernmentAssessmentResult;
import com.odonta.polity.result.GovernmentReadinessResult;
import com.odonta.polity.service.MembershipService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GovernmentAssessmentResolver {
  private static final int MINIMUM_FULL_GOVERNMENT_MEMBERS = 3;

  private final Clock clock;
  private final ConstitutionalPowerRepository powers;
  private final MembershipRepository memberships;
  private final MembershipService membershipService;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final ProcedureElectorateResolver procedureElectorates;
  private final ProcedureRepository procedures;

  public int minimumFullGovernmentMembers() {
    return MINIMUM_FULL_GOVERNMENT_MEMBERS;
  }

  public long activeMemberCount(UUID polityId) {
    return memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE);
  }

  public long standingMemberCount(UUID polityId) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    return memberships
        .findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(polityId, MembershipStatus.ACTIVE)
        .stream()
        .filter(member -> membershipService.hasPoliticalStanding(member.getId(), now))
        .count();
  }

  public boolean hasFullGovernmentSize(UUID polityId) {
    return standingMemberCount(polityId) >= minimumFullGovernmentMembers();
  }

  public GovernmentAssessmentResult assess(Polity polity, ConstitutionVersion constitution) {
    if (polity.isDisbanded()) {
      return new GovernmentAssessmentResult(
          new GovernmentReadinessResult(
              GovernmentReadinessStatus.DISBANDED,
              List.of(GovernmentReadinessDiagnostic.POLITY_DISBANDED)),
          new ConstitutionalHealthResult(
              ConstitutionalHealthStatus.CRITICAL,
              List.of(ConstitutionalHealthDiagnostic.POLITY_DISBANDED)));
    }
    return new GovernmentAssessmentResult(
        readiness(polity.getId(), constitution), health(polity.getId(), constitution));
  }

  private GovernmentReadinessResult readiness(UUID polityId, ConstitutionVersion constitution) {
    List<GovernmentReadinessDiagnostic> diagnostics = new ArrayList<>();
    if (!hasFullGovernmentSize(polityId)) {
      diagnostics.add(GovernmentReadinessDiagnostic.NEEDS_MORE_STANDING_MEMBERS);
      return new GovernmentReadinessResult(GovernmentReadinessStatus.PROVISIONAL, diagnostics);
    }
    addReadinessDiagnostics(
        diagnostics,
        polityId,
        constitution,
        ConstitutionalMotionPath.OFFICE_ELECTION,
        GovernmentReadinessDiagnostic.OFFICE_ELECTION_AUTHORITY_UNAVAILABLE,
        GovernmentReadinessDiagnostic.OFFICE_ELECTION_ELECTORATE_UNAVAILABLE);
    if (!holderAvailable(polityId, constitution, PowerCode.REQUEST_CERTIFICATION)) {
      diagnostics.add(GovernmentReadinessDiagnostic.CERTIFICATION_AUTHORITY_UNAVAILABLE);
    }
    addReadinessDiagnostics(
        diagnostics,
        polityId,
        constitution,
        ConstitutionalMotionPath.ORDINARY_GOVERNANCE,
        GovernmentReadinessDiagnostic.ORDINARY_GOVERNANCE_AUTHORITY_UNAVAILABLE,
        GovernmentReadinessDiagnostic.ORDINARY_GOVERNANCE_ELECTORATE_UNAVAILABLE);
    addReadinessDiagnostics(
        diagnostics,
        polityId,
        constitution,
        ConstitutionalMotionPath.APPEAL,
        GovernmentReadinessDiagnostic.APPEAL_AUTHORITY_UNAVAILABLE,
        GovernmentReadinessDiagnostic.APPEAL_ELECTORATE_UNAVAILABLE);
    addReadinessDiagnostics(
        diagnostics,
        polityId,
        constitution,
        ConstitutionalMotionPath.CONSTITUTIONAL_REVIEW,
        GovernmentReadinessDiagnostic.CONSTITUTIONAL_REVIEW_AUTHORITY_UNAVAILABLE,
        GovernmentReadinessDiagnostic.CONSTITUTIONAL_REVIEW_ELECTORATE_UNAVAILABLE);
    if (diagnostics.contains(GovernmentReadinessDiagnostic.OFFICE_ELECTION_AUTHORITY_UNAVAILABLE)
        || diagnostics.contains(
            GovernmentReadinessDiagnostic.OFFICE_ELECTION_ELECTORATE_UNAVAILABLE)
        || diagnostics.contains(
            GovernmentReadinessDiagnostic.CERTIFICATION_AUTHORITY_UNAVAILABLE)) {
      return new GovernmentReadinessResult(GovernmentReadinessStatus.BLOCKED, diagnostics);
    }
    if (diagnostics.isEmpty()) {
      return new GovernmentReadinessResult(GovernmentReadinessStatus.READY, diagnostics);
    }
    return new GovernmentReadinessResult(GovernmentReadinessStatus.FORMING_OFFICES, diagnostics);
  }

  private ConstitutionalHealthResult health(UUID polityId, ConstitutionVersion constitution) {
    List<ConstitutionalHealthDiagnostic> diagnostics = new ArrayList<>();
    addIfUnavailable(
        diagnostics,
        ConstitutionalHealthDiagnostic.ADMISSION_PATH_UNAVAILABLE,
        powerStructurallyAvailable(constitution, PowerCode.ADMIT_MEMBER));
    addIfUnavailable(
        diagnostics,
        ConstitutionalHealthDiagnostic.ORDINARY_GOVERNANCE_UNAVAILABLE,
        pathStructurallyAvailable(constitution, ConstitutionalMotionPath.ORDINARY_GOVERNANCE));
    addIfUnavailable(
        diagnostics,
        ConstitutionalHealthDiagnostic.SANCTION_PATH_UNAVAILABLE,
        pathStructurallyAvailable(constitution, ConstitutionalMotionPath.SANCTION));
    addIfUnavailable(
        diagnostics,
        ConstitutionalHealthDiagnostic.APPEAL_PATH_UNAVAILABLE,
        pathStructurallyAvailable(constitution, ConstitutionalMotionPath.APPEAL));
    addIfUnavailable(
        diagnostics,
        ConstitutionalHealthDiagnostic.OFFICE_TERM_REVIEW_PATH_UNAVAILABLE,
        pathStructurallyAvailable(constitution, ConstitutionalMotionPath.OFFICE_TERM_REVIEW));
    addIfUnavailable(
        diagnostics,
        ConstitutionalHealthDiagnostic.CONSTITUTIONAL_REVIEW_PATH_UNAVAILABLE,
        pathStructurallyAvailable(constitution, ConstitutionalMotionPath.CONSTITUTIONAL_REVIEW));
    addIfUnavailable(
        diagnostics,
        ConstitutionalHealthDiagnostic.OFFICE_ELECTION_PATH_UNAVAILABLE,
        pathStructurallyAvailable(constitution, ConstitutionalMotionPath.OFFICE_ELECTION));
    addIfUnavailable(
        diagnostics,
        ConstitutionalHealthDiagnostic.AMENDMENT_PATH_UNAVAILABLE,
        pathStructurallyAvailable(constitution, ConstitutionalMotionPath.CONSTITUTION_AMENDMENT));
    addIfUnavailable(
        diagnostics,
        ConstitutionalHealthDiagnostic.DISBANDMENT_PATH_UNAVAILABLE,
        pathStructurallyAvailable(constitution, ConstitutionalMotionPath.DISBANDMENT));
    addIfUnavailable(
        diagnostics,
        ConstitutionalHealthDiagnostic.CERTIFICATION_PATH_UNAVAILABLE,
        powerStructurallyAvailable(constitution, PowerCode.REQUEST_CERTIFICATION));
    if (diagnostics.isEmpty()) {
      return new ConstitutionalHealthResult(ConstitutionalHealthStatus.HEALTHY, diagnostics);
    }
    if (diagnostics.stream().anyMatch(this::isCritical)) {
      return new ConstitutionalHealthResult(ConstitutionalHealthStatus.CRITICAL, diagnostics);
    }
    return new ConstitutionalHealthResult(ConstitutionalHealthStatus.DEGRADED, diagnostics);
  }

  private void addIfUnavailable(
      List<ConstitutionalHealthDiagnostic> diagnostics,
      ConstitutionalHealthDiagnostic diagnostic,
      boolean available) {
    if (!available) {
      diagnostics.add(diagnostic);
    }
  }

  private boolean isCritical(ConstitutionalHealthDiagnostic diagnostic) {
    return switch (diagnostic) {
      case OFFICE_ELECTION_PATH_UNAVAILABLE,
          AMENDMENT_PATH_UNAVAILABLE,
          DISBANDMENT_PATH_UNAVAILABLE,
          CERTIFICATION_PATH_UNAVAILABLE,
          POLITY_DISBANDED ->
          true;
      case ADMISSION_PATH_UNAVAILABLE,
          ORDINARY_GOVERNANCE_UNAVAILABLE,
          SANCTION_PATH_UNAVAILABLE,
          APPEAL_PATH_UNAVAILABLE,
          OFFICE_TERM_REVIEW_PATH_UNAVAILABLE,
          CONSTITUTIONAL_REVIEW_PATH_UNAVAILABLE ->
          false;
    };
  }

  private void addReadinessDiagnostics(
      List<GovernmentReadinessDiagnostic> diagnostics,
      UUID polityId,
      ConstitutionVersion constitution,
      ConstitutionalMotionPath path,
      GovernmentReadinessDiagnostic authorityDiagnostic,
      GovernmentReadinessDiagnostic electorateDiagnostic) {
    if (!holderAvailable(polityId, constitution, path.introducingPower())) {
      diagnostics.add(authorityDiagnostic);
      return;
    }
    if (!procedureAvailability(polityId, constitution, path.procedureCode()).available()) {
      diagnostics.add(electorateDiagnostic);
    }
  }

  public ActionAvailabilityResult procedureAvailability(
      UUID polityId, ConstitutionVersion constitution, String procedureCode) {
    return procedures
        .findEntityByConstitutionVersionIdAndCode(constitution.getId(), procedureCode)
        .map(this::procedureElectorateAvailability)
        .orElse(ActionAvailabilityResult.blocked(ActionUnavailableReason.PROCEDURE_MISSING));
  }

  public boolean lastMemberResignationClosesPolity(
      Polity polity, ConstitutionVersion constitution) {
    ConstitutionalMotionPath path = ConstitutionalMotionPath.DISBANDMENT;
    return !pathStructurallyAvailable(constitution, path)
        || !holderAvailable(polity.getId(), constitution, path.introducingPower())
        || !procedureAvailability(polity.getId(), constitution, path.procedureCode()).available();
  }

  private boolean holderAvailable(UUID polityId, ConstitutionVersion constitution, PowerCode code) {
    return powers
        .findEntityByConstitutionVersionIdAndCode(constitution.getId(), code)
        .map(power -> powerHolderAvailable(polityId, power))
        .orElse(false);
  }

  private boolean pathStructurallyAvailable(
      ConstitutionVersion constitution, ConstitutionalMotionPath path) {
    return powerStructurallyAvailable(constitution, path.introducingPower())
        && procedureStructurallyAvailable(constitution, path.procedureCode());
  }

  private boolean powerStructurallyAvailable(ConstitutionVersion constitution, PowerCode code) {
    return powers
        .findEntityByConstitutionVersionIdAndCode(constitution.getId(), code)
        .map(power -> powerHolderStructurallyAvailable(constitution, power))
        .orElse(false);
  }

  private boolean powerHolderStructurallyAvailable(
      ConstitutionVersion constitution, ConstitutionalPower power) {
    return switch (power.getHolderScope()) {
      case ACTIVE_MEMBER -> true;
      case OFFICE ->
          offices.existsByConstitutionVersionIdAndCode(
              constitution.getId(), power.getHolderOfficeCode());
    };
  }

  private boolean powerHolderAvailable(UUID polityId, ConstitutionalPower power) {
    return switch (power.getHolderScope()) {
      case ACTIVE_MEMBER -> standingMemberCount(polityId) > 0;
      case OFFICE -> hasStandingOfficeHolder(polityId, power.getHolderOfficeCode());
    };
  }

  private boolean procedureStructurallyAvailable(
      ConstitutionVersion constitution, String procedureCode) {
    return procedures
        .findEntityByConstitutionVersionIdAndCode(constitution.getId(), procedureCode)
        .map(procedure -> procedureElectorateStructurallyAvailable(constitution, procedure))
        .orElse(false);
  }

  private boolean procedureElectorateStructurallyAvailable(
      ConstitutionVersion constitution, Procedure procedure) {
    if (procedure.getElectorate() == ProcedureElectorate.ACTIVE_MEMBERS) {
      return true;
    }
    return offices
        .findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), procedure.getElectorateOfficeCode())
        .map(office -> office.getSeatCount() >= procedure.getMinimumElectorCount())
        .orElse(false);
  }

  private ActionAvailabilityResult procedureElectorateAvailability(Procedure procedure) {
    OffsetDateTime votingOpensAt =
        OffsetDateTime.now(clock).plusHours(procedure.getMinimumNoticeHours());
    List<Membership> electors = procedureElectorates.electors(procedure, votingOpensAt);
    if (electors.isEmpty()) {
      return procedure.getElectorate() == ProcedureElectorate.OFFICE_HOLDERS
          ? ActionAvailabilityResult.blocked(
              ActionUnavailableReason.PROCEDURE_ELECTORATE_OFFICE_VACANT)
          : ActionAvailabilityResult.blocked(ActionUnavailableReason.PROCEDURE_ELECTORATE_EMPTY);
    }
    if (electors.size() < procedure.getMinimumElectorCount()) {
      return ActionAvailabilityResult.blocked(
          ActionUnavailableReason.PROCEDURE_ELECTORATE_BELOW_MINIMUM);
    }
    return ActionAvailabilityResult.allowed();
  }

  private boolean hasStandingOfficeHolder(UUID polityId, String officeCode) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    return officeTerms
        .findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
            polityId, officeCode, OfficeTermStatus.ACTIVE, now)
        .stream()
        .map(OfficeTerm::getMembershipId)
        .map(this::membership)
        .anyMatch(member -> membershipService.hasPoliticalStanding(member.getId(), now));
  }

  private Membership membership(UUID membershipId) {
    return memberships.findEntityById(membershipId).orElseThrow(PolityResource.MEMBER::notFound);
  }
}
