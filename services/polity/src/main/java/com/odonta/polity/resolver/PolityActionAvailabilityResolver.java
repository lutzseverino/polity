package com.odonta.polity.resolver;

import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.repository.AppealProposalProjection;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProjection;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.result.ActionAvailabilityResult;
import com.odonta.polity.result.GovernmentAssessmentResult;
import com.odonta.polity.result.PolityActionAvailabilityResult;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolityActionAvailabilityResolver {
  private final Clock clock;
  private final PolityAccessPolicy access;
  private final AppealProposalRepository appealProposals;
  private final AppealRepository appeals;
  private final ConstitutionalAuthority authority;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalPowerRepository powers;
  private final GovernmentAssessmentResolver governmentAssessments;
  private final MembershipRepository memberships;
  private final MotionRepository motions;
  private final OfficeTermRepository officeTerms;
  private final PolityRepository polities;
  private final ProcedureElectorateResolver procedureElectorates;
  private final ProcedureRepository procedures;
  private final SanctionRepository sanctions;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PolityActionAvailabilityResult resolve(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    Polity polity = polity(polityId);
    ConstitutionVersion constitution = constitution(polityId);
    GovernmentAssessmentResult assessment = governmentAssessments.assess(polity, constitution);
    if (polity.isDisbanded()) {
      return unavailable(assessment, "polity_disbanded");
    }
    Membership member =
        memberships
            .findEntityByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
            .orElse(null);
    if (member == null) {
      return unavailable(assessment, "polity_membership_required");
    }
    return new PolityActionAvailabilityResult(
        assessment.readiness(),
        assessment.constitutionalHealth(),
        invitationAvailability(member, constitution),
        fullGovernmentProcedureAvailability(
            member, constitution, PowerCode.INTRODUCE_MOTION, Procedure.ORDINARY_RESOLUTION),
        fullGovernmentProcedureAvailability(
            member, constitution, PowerCode.INTRODUCE_OFFICE_ELECTION, Procedure.OFFICE_ELECTION),
        sanctionAvailability(member, constitution),
        appealAvailability(member, constitution),
        fullGovernmentProcedureAvailability(
            member,
            constitution,
            PowerCode.INTRODUCE_OFFICE_TERM_REVIEW,
            Procedure.OFFICE_TERM_REVIEW),
        fullGovernmentProcedureAvailability(
            member,
            constitution,
            PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW,
            Procedure.CONSTITUTIONAL_REVIEW),
        fullGovernmentProcedureAvailability(
            member, constitution, PowerCode.INTRODUCE_AMENDMENT, Procedure.CONSTITUTION_AMENDMENT),
        disbandmentAvailability(member, constitution),
        certificationAvailability(member, constitution),
        resignationAvailability(polity, member, constitution));
  }

  public boolean hasProvisionalFounderAdmissionAuthority(Membership member) {
    Polity polity = polity(member.getPolityId());
    if (polity.getStatus() != PolityStatus.ACTIVE) {
      throw ApiException.conflict(
          "polity_disbanded", "This polity has been disbanded and no longer accepts actions.");
    }
    if (polity.isBootstrapComplete()) {
      return false;
    }
    return !governmentAssessments.hasFullGovernmentSize(member.getPolityId())
        && polity.getFounderId().equals(member.getUserId())
        && member.getStatus() == MembershipStatus.ACTIVE;
  }

  public ActionAvailabilityResult sanctionAvailability(
      Membership member, ConstitutionVersion constitution) {
    ActionAvailabilityResult authorityResult =
        authorityAvailability(member, constitution, PowerCode.INTRODUCE_SANCTION);
    if (!authorityResult.available()) {
      return authorityResult;
    }
    ActionAvailabilityResult appealAvailability =
        governmentAssessments.procedureAvailability(
            member.getPolityId(), constitution, Procedure.APPEAL);
    return appealAvailability.available()
        ? ActionAvailabilityResult.allowed()
        : ActionAvailabilityResult.blocked("appeal_procedure_unavailable");
  }

  private PolityActionAvailabilityResult unavailable(
      GovernmentAssessmentResult assessment, String reason) {
    ActionAvailabilityResult unavailable = ActionAvailabilityResult.blocked(reason);
    return new PolityActionAvailabilityResult(
        assessment.readiness(),
        assessment.constitutionalHealth(),
        unavailable,
        unavailable,
        unavailable,
        unavailable,
        unavailable,
        unavailable,
        unavailable,
        unavailable,
        unavailable,
        unavailable,
        unavailable);
  }

  private ActionAvailabilityResult invitationAvailability(
      Membership member, ConstitutionVersion constitution) {
    ActionAvailabilityResult authorityResult =
        authorityAvailability(member, constitution, PowerCode.ADMIT_MEMBER);
    if (authorityResult.available()
        || !authorityResult.reason().equals("constitutional_authority_missing")) {
      return authorityResult;
    }
    return hasProvisionalFounderAdmissionAuthority(member)
        ? ActionAvailabilityResult.allowed()
        : authorityResult;
  }

  private ActionAvailabilityResult appealAvailability(
      Membership member, ConstitutionVersion constitution) {
    ActionAvailabilityResult authorityResult =
        fullGovernmentProcedureAvailability(
            member, constitution, PowerCode.INTRODUCE_APPEAL, Procedure.APPEAL);
    if (authorityResult.available() || !hasOwnAppealAvailable(member, constitution)) {
      return authorityResult;
    }
    return ActionAvailabilityResult.allowed();
  }

  private boolean hasOwnAppealAvailable(Membership member, ConstitutionVersion constitution) {
    try {
      if (!authority.allowsOwnAppealIntroduction(member, constitution)) {
        return false;
      }
    } catch (ApiException exception) {
      return false;
    }
    Procedure appealProcedure =
        procedures
            .findEntityByConstitutionVersionIdAndCode(constitution.getId(), Procedure.APPEAL)
            .orElse(null);
    if (appealProcedure == null) {
      return false;
    }
    OffsetDateTime now = OffsetDateTime.now(clock);
    return sanctions
        .findProjectionsByPolityIdAndTargetMembershipIdAndStatusAndEndsAtAfterOrderByStartedAtDesc(
            member.getPolityId(), member.getId(), SanctionStatus.ACTIVE, now)
        .stream()
        .anyMatch(sanction -> isOwnAppealAvailable(member, sanction, appealProcedure, now));
  }

  private boolean isOwnAppealAvailable(
      Membership member,
      SanctionProjection sanction,
      Procedure appealProcedure,
      OffsetDateTime now) {
    if (appeals.existsByPolityIdAndSanctionId(member.getPolityId(), sanction.getId())
        || hasOpenAppealProposal(member.getPolityId(), sanction.getId())) {
      return false;
    }
    return motions
        .findEntityByIdAndPolityId(sanction.getMotionId(), member.getPolityId())
        .map(Motion::getIntroducedBy)
        .map(
            sanctionIntroducerId ->
                appealElectorateAvailable(
                    appealProcedure, now, member.getId(), sanctionIntroducerId))
        .orElse(false);
  }

  private boolean appealElectorateAvailable(
      Procedure appealProcedure,
      OffsetDateTime now,
      UUID targetMembershipId,
      UUID sanctionIntroducerId) {
    OffsetDateTime votingOpensAt = now.plusHours(appealProcedure.getMinimumNoticeHours());
    long eligible =
        procedureElectorates.electors(appealProcedure, votingOpensAt).stream()
            .map(Membership::getId)
            .filter(membershipId -> !membershipId.equals(targetMembershipId))
            .filter(membershipId -> !membershipId.equals(sanctionIntroducerId))
            .count();
    return eligible >= appealProcedure.getMinimumElectorCount();
  }

  private boolean hasOpenAppealProposal(UUID polityId, UUID sanctionId) {
    return appealProposals.findProjectionsByPolityIdAndSanctionId(polityId, sanctionId).stream()
        .map(AppealProposalProjection::getMotionId)
        .anyMatch(motionId -> motions.existsByIdAndStatus(motionId, MotionStatus.VOTING));
  }

  private ActionAvailabilityResult certificationAvailability(
      Membership member, ConstitutionVersion constitution) {
    ActionAvailabilityResult authorityResult =
        authorityAvailability(member, constitution, PowerCode.REQUEST_CERTIFICATION);
    if (authorityResult.available() || !hasOwnAppealCertificationAvailable(member)) {
      return authorityResult;
    }
    return ActionAvailabilityResult.allowed();
  }

  private boolean hasOwnAppealCertificationAvailable(Membership member) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    return appealProposals
        .findProjectionsByPolityIdAndAppellantMembershipId(member.getPolityId(), member.getId())
        .stream()
        .anyMatch(proposal -> isOwnAppealCertificationAvailable(member, proposal, now));
  }

  private boolean isOwnAppealCertificationAvailable(
      Membership member, AppealProposalProjection proposal, OffsetDateTime now) {
    Motion motion =
        motions
            .findEntityByIdAndPolityId(proposal.getMotionId(), member.getPolityId())
            .orElse(null);
    if (motion == null
        || motion.getStatus() != MotionStatus.VOTING
        || now.isBefore(motion.getCertificationOpensAt())) {
      return false;
    }
    ConstitutionVersion motionConstitution =
        constitutions.findEntityById(motion.getConstitutionVersionId()).orElse(null);
    if (motionConstitution == null
        || motionConstitution.getStatus() != ConstitutionStatus.RATIFIED) {
      return false;
    }
    try {
      return authority.allowsAppealCertification(member, motionConstitution);
    } catch (ApiException exception) {
      return false;
    }
  }

  private ActionAvailabilityResult fullGovernmentProcedureAvailability(
      Membership member,
      ConstitutionVersion constitution,
      PowerCode powerCode,
      String procedureCode) {
    ActionAvailabilityResult authorityResult =
        authorityAvailability(member, constitution, powerCode);
    if (!authorityResult.available()) {
      return authorityResult;
    }
    return governmentAssessments.procedureAvailability(
        member.getPolityId(), constitution, procedureCode);
  }

  private ActionAvailabilityResult disbandmentAvailability(
      Membership member, ConstitutionVersion constitution) {
    ActionAvailabilityResult authorityResult =
        authorityAvailability(member, constitution, PowerCode.INTRODUCE_DISBANDMENT);
    if (!authorityResult.available()) {
      return authorityResult;
    }
    return governmentAssessments.procedureAvailability(
        member.getPolityId(), constitution, Procedure.DISBANDMENT);
  }

  private ActionAvailabilityResult resignationAvailability(
      Polity polity, Membership member, ConstitutionVersion constitution) {
    if (governmentAssessments.activeMemberCount(member.getPolityId()) <= 1
        && governmentAssessments.lastMemberResignationClosesPolity(polity, constitution)) {
      return ActionAvailabilityResult.allowed();
    }
    if (!polity.isBootstrapComplete() && polity.getFounderId().equals(member.getUserId())) {
      return ActionAvailabilityResult.blocked("provisional_founder_resignation_unavailable");
    }
    return governmentAssessments.activeMemberCount(member.getPolityId()) <= 1
        ? ActionAvailabilityResult.blocked("last_member_resignation_unavailable")
        : ActionAvailabilityResult.allowed();
  }

  private ActionAvailabilityResult authorityAvailability(
      Membership member, ConstitutionVersion constitution, PowerCode powerCode) {
    try {
      ConstitutionalPower power =
          powers
              .findEntityByConstitutionVersionIdAndCode(constitution.getId(), powerCode)
              .orElseThrow(
                  () ->
                      ApiException.forbidden(
                          "constitutional_power_missing",
                          "The governing constitution does not authorize this action."));
      if (power.getHolderScope() == PowerHolderScope.OFFICE
          && !officeTerms.existsByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
              member.getPolityId(),
              power.getHolderOfficeCode(),
              OfficeTermStatus.ACTIVE,
              OffsetDateTime.now(clock))) {
        return ActionAvailabilityResult.blocked("constitutional_office_vacant");
      }
      return authority.allows(member, constitution, powerCode)
          ? ActionAvailabilityResult.allowed()
          : ActionAvailabilityResult.blocked("constitutional_authority_missing");
    } catch (ApiException exception) {
      return ActionAvailabilityResult.blocked(exception.code());
    }
  }

  private ConstitutionVersion constitution(UUID polityId) {
    return constitutions
        .findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED)
        .orElseThrow(
            () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
  }

  private Polity polity(UUID polityId) {
    return polities
        .findEntityById(polityId)
        .orElseThrow(() -> ApiException.notFound("polity_not_found", "Polity not found."));
  }
}
