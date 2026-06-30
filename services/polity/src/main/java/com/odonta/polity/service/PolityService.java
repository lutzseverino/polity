package com.odonta.polity.service;

import com.odonta.authorization.grant.Grants;
import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.billing.client.BillingEntitlement;
import com.odonta.billing.client.BillingEntitlementsClient;
import com.odonta.common.api.ApiException;
import com.odonta.identity.client.IdentityUser;
import com.odonta.identity.client.IdentityUsersClient;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.mapper.OfficeApplicationMapper;
import com.odonta.polity.mapper.PolityApplicationMapper;
import com.odonta.polity.model.ActionAvailabilityResult;
import com.odonta.polity.model.ConstitutionProcedureResult;
import com.odonta.polity.model.ConstitutionResult;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalHealthDiagnostic;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.CreatePolityInput;
import com.odonta.polity.model.GovernmentAssessmentResult;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityActionAvailabilityResult;
import com.odonta.polity.model.PolityPace;
import com.odonta.polity.model.PolityResult;
import com.odonta.polity.model.PolitySetupPreset;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.AppealProposalProjection;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProjection;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.resolver.GovernmentAssessmentResolver;
import com.odonta.polity.resolver.ProcedureElectorateResolver;
import com.odonta.polity.template.ConstitutionTemplateSeeder;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class PolityService {
  private final Clock clock;
  private final PolityAccessPolicy access;
  private final AppealProposalRepository appealProposals;
  private final AppealRepository appeals;
  private final BillingEntitlementsClient entitlements;
  private final ConstitutionalAuthority authority;
  private final ConstitutionTemplateSeeder templates;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalPowerRepository powers;
  private final Grants grants;
  private final GovernmentAssessmentResolver governmentAssessments;
  private final IdentityUsersClient identityUsers;
  private final PolityApplicationMapper mapper;
  private final InstitutionRepository institutions;
  private final JurisdictionRepository jurisdictions;
  private final MembershipService membershipService;
  private final MembershipRepository memberships;
  private final MotionRepository motions;
  private final OfficeApplicationMapper officeMapper;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficialRecordService officialRecords;
  private final PolityGrantPlanner grantPlanner;
  private final PolityRepository polities;
  private final ProcedureElectorateResolver procedureElectorates;
  private final ProcedureRepository procedures;
  private final SanctionRepository sanctions;

  @Transactional
  public void provisionAccount(AuthenticatedUser user) {
    grants.stage(grantPlanner.account(user.authorizationSubject()));
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_CREATE)
  public PolityResult create(AuthenticatedUser founder, @Valid CreatePolityInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    requirePrivatePolityAvailability(founder.id(), input.visibility());
    IdentityUser identity = identityUsers.get(founder.id());
    PolitySetupPreset setupPreset = input.setupPresetOrDefault();
    PolityPace pace = input.paceOrDefault();
    Polity polity =
        polities.saveAndFlush(new Polity(input.name(), input.visibility(), founder.id()));
    Jurisdiction jurisdiction =
        jurisdictions.saveAndFlush(
            new Jurisdiction(polity.getId(), input.name(), JurisdictionKind.ROOT));
    ConstitutionVersion constitution =
        constitutions.saveAndFlush(
            new ConstitutionVersion(
                polity.getId(),
                1,
                ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
                ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
                ConstitutionTemplateKey.STRUCTURED_CHARTER,
                now));
    if (setupPreset == PolitySetupPreset.STANDARD_CONSTITUTIONAL_COUNCIL_REPUBLIC) {
      templates.establishStandardConstitutionalCouncilRepublic(jurisdiction, constitution, pace);
    }
    Membership membership =
        memberships.saveAndFlush(
            new Membership(
                polity.getId(),
                identity.id(),
                identity.authorizationSubject(),
                identity.email(),
                displayName(identity),
                now,
                null));
    Office steward =
        offices
            .findEntityByConstitutionVersionIdAndCode(constitution.getId(), Office.STEWARD)
            .orElseThrow(() -> ApiException.notFound("office_not_found", "Office not found."));
    OfficeTerm stewardTerm =
        officeTerms.saveAndFlush(
            new OfficeTerm(
                polity.getId(),
                steward.getId(),
                steward.getCode(),
                membership.getId(),
                now,
                now.plusDays(steward.getTermLengthDays())));
    grants.stage(grantPlanner.membership(founder.authorizationSubject(), polity.getId()));
    officialRecords.append(
        polity.getId(),
        jurisdiction.getId(),
        constitution.getId(),
        membership.getId(),
        OfficialRecordType.POLITY_FOUNDED,
        polity.getId(),
        OfficialRecordContext.none(),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.POLITY_FOUNDED,
            TemplateParameters.of(
                "polityName",
                input.name(),
                "setupPreset",
                setupPreset.name(),
                "setupPresetKey",
                setupPreset.labelKey(),
                "pace",
                pace.name(),
                "paceKey",
                pace.labelKey())),
        now);
    officialRecords.append(
        polity.getId(),
        jurisdiction.getId(),
        constitution.getId(),
        membership.getId(),
        OfficialRecordType.OFFICE_ASSIGNED,
        stewardTerm.getId(),
        OfficialRecordContext.none(),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.BOOTSTRAP_STEWARD_ASSIGNED,
            TemplateParameters.of(
                "memberName",
                membership.getDisplayName(),
                "officeName",
                steward.getName(),
                "officeNameKey",
                steward.getNameKey(),
                "officeCode",
                steward.getCode(),
                "termLengthDays",
                steward.getTermLengthDays(),
                "constitutionName",
                ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
                "constitutionTitleKey",
                ConstitutionTemplateKey.STRUCTURED_CHARTER.titleKey())),
        now);
    officialRecords.append(
        polity.getId(),
        jurisdiction.getId(),
        constitution.getId(),
        membership.getId(),
        OfficialRecordType.CONSTITUTION_RATIFIED,
        constitution.getId(),
        OfficialRecordContext.none(),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.CONSTITUTION_RATIFIED,
            TemplateParameters.of(
                "constitutionName",
                ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
                "constitutionTitleKey",
                ConstitutionTemplateKey.STRUCTURED_CHARTER.titleKey(),
                "constitutionBody",
                ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
                "constitutionBodyKey",
                ConstitutionTemplateKey.STRUCTURED_CHARTER.bodyKey())),
        now);
    return getSummary(polity.getId());
  }

  private void requirePrivatePolityAvailability(UUID founderId, PolityVisibility visibility) {
    if (visibility != PolityVisibility.PRIVATE) {
      return;
    }
    BillingEntitlement entitlement = entitlements.require(founderId, PolityResources.PRODUCT);
    polities.lockFounderPrivatePolityQuota(founderId);
    if (entitlement.tenantLimit() != null
        && polities.countByFounderIdAndVisibilityAndStatus(
                founderId, PolityVisibility.PRIVATE, PolityStatus.ACTIVE)
            >= entitlement.tenantLimit()) {
      throw ApiException.conflict(
          "private_polity_limit_reached", "Private polity entitlement limit has been reached.");
    }
  }

  public List<PolityResult> list(UUID userId) {
    return polities
        .findAccessibleProjections(userId, MembershipStatus.ACTIVE, PolityVisibility.PUBLIC)
        .stream()
        .map(this::summary)
        .toList();
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PolityResult get(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return getSummary(polityId);
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public ConstitutionResult getConstitution(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    ConstitutionVersion constitution = constitution(polityId);
    Polity polity = polity(polityId);
    return mapper.toConstitutionResult(
        constitution,
        jurisdictions.findProjectionsByPolityId(polityId).stream()
            .map(mapper::toResult)
            .sorted(java.util.Comparator.comparing(result -> result.kind().name()))
            .toList(),
        institutions.findProjectionsByConstitutionVersionId(constitution.getId()).stream()
            .map(mapper::toResult)
            .sorted(java.util.Comparator.comparing(result -> result.kind().name()))
            .toList(),
        procedures.findProjectionsByConstitutionVersionId(constitution.getId()).stream()
            .map(mapper::toResult)
            .sorted(java.util.Comparator.comparing(ConstitutionProcedureResult::code))
            .toList(),
        offices.findProjectionsByConstitutionVersionIdOrderByName(constitution.getId()).stream()
            .map(officeMapper::toResult)
            .toList(),
        powers.findProjectionsByConstitutionVersionId(constitution.getId()).stream()
            .map(mapper::toResult)
            .sorted(java.util.Comparator.comparing(result -> result.code().name()))
            .toList(),
        mapper.toBootstrapResult(
            polity.isBootstrapComplete(),
            polity.getBootstrapCompletedAt(),
            governmentAssessments.minimumFullGovernmentMembers(),
            governmentAssessments.activeMemberCount(polityId),
            governmentAssessments.standingMemberCount(polityId)));
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PolityActionAvailabilityResult getActionAvailability(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    Polity polity = polity(polityId);
    ConstitutionVersion constitution = constitution(polityId);
    GovernmentAssessmentResult assessment = governmentAssessments.assess(polity, constitution);
    if (polity.isDisbanded()) {
      ActionAvailabilityResult unavailable = ActionAvailabilityResult.blocked("polity_disbanded");
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
    Membership member =
        memberships
            .findEntityByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
            .orElse(null);
    if (member == null) {
      ActionAvailabilityResult unavailable =
          ActionAvailabilityResult.blocked("polity_membership_required");
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
        resignationAvailability(polity, member, assessment));
  }

  private String displayName(IdentityUser user) {
    return user.name() == null || user.name().isBlank() ? user.email() : user.name();
  }

  private PolityResult getSummary(UUID polityId) {
    return summary(
        polities
            .findProjectedById(polityId)
            .orElseThrow(() -> ApiException.notFound("polity_not_found", "Polity not found.")));
  }

  private PolityResult summary(PolityProjection projection) {
    ConstitutionVersion constitution = constitution(projection.getId());
    Jurisdiction jurisdiction = jurisdiction(projection.getId());
    Institution institution = institution(projection.getId(), constitution);
    return mapper.toResult(
        projection,
        constitution.getVersion(),
        jurisdiction.getName(),
        institution.getName(),
        institution.getNameKey());
  }

  ConstitutionVersion constitution(UUID polityId) {
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

  Jurisdiction jurisdiction(UUID polityId) {
    return jurisdictions
        .findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT)
        .orElseThrow(
            () -> ApiException.notFound("jurisdiction_not_found", "Jurisdiction not found."));
  }

  Institution institution(UUID polityId) {
    return institution(polityId, constitution(polityId));
  }

  Institution institution(UUID polityId, ConstitutionVersion constitution) {
    List<Institution> current =
        institutions.findEntitiesByPolityIdAndConstitutionVersionId(polityId, constitution.getId());
    return current.stream()
        .filter(institution -> institution.getKind() == InstitutionKind.COUNCIL)
        .findFirst()
        .or(
            () ->
                current.stream()
                    .filter(institution -> institution.getKind() == InstitutionKind.ASSEMBLY)
                    .findFirst())
        .or(() -> current.stream().findFirst())
        .orElseThrow(
            () -> ApiException.notFound("institution_not_found", "Institution not found."));
  }

  Institution institution(UUID polityId, Procedure procedure) {
    return institutions
        .findEntityByIdAndPolityId(procedure.getInstitutionId(), polityId)
        .orElseThrow(
            () -> ApiException.notFound("institution_not_found", "Institution not found."));
  }

  void requireActive(UUID polityId) {
    Polity polity = polity(polityId);
    if (polity.getStatus() != PolityStatus.ACTIVE) {
      throw ApiException.conflict(
          "polity_disbanded", "This polity has been disbanded and no longer accepts actions.");
    }
  }

  String name(UUID polityId) {
    return polities
        .findProjectedById(polityId)
        .map(com.odonta.polity.repository.PolityProjection::getName)
        .orElseThrow(() -> ApiException.notFound("polity_not_found", "Polity not found."));
  }

  void requireDisbandmentGovernment(UUID polityId) {
    requireActive(polityId);
  }

  void completeBootstrapIfReady(UUID polityId, OffsetDateTime now) {
    Polity polity = polity(polityId);
    if (polity.isBootstrapComplete() || !governmentAssessments.hasFullGovernmentSize(polityId)) {
      return;
    }
    polity.completeBootstrap(now);
    polities.saveAndFlush(polity);
    List<OfficeTerm> bootstrapTerms =
        officeTerms.findEntitiesByPolityIdAndOfficeCodeAndStatusAndAssignedByMotionIdIsNull(
            polityId, Office.STEWARD, OfficeTermStatus.ACTIVE);
    bootstrapTerms.forEach(term -> term.end(now));
    if (!bootstrapTerms.isEmpty()) {
      officeTerms.saveAllAndFlush(bootstrapTerms);
    }
  }

  boolean hasProvisionalFounderAdmissionAuthority(Membership member) {
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

  private long standingMemberCount(UUID polityId) {
    return governmentAssessments.standingMemberCount(polityId);
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

  ActionAvailabilityResult sanctionAvailability(
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

  private ActionAvailabilityResult disbandmentAvailability(
      Membership member, ConstitutionVersion constitution) {
    ActionAvailabilityResult authorityResult =
        authorityAvailability(member, constitution, PowerCode.INTRODUCE_DISBANDMENT);
    if (!authorityResult.available()) {
      return authorityResult;
    }
    ActionAvailabilityResult governmentAvailability = disbandmentGovernmentAvailability();
    return governmentAvailability.available()
        ? governmentAssessments.procedureAvailability(
            member.getPolityId(), constitution, Procedure.DISBANDMENT)
        : governmentAvailability;
  }

  private ActionAvailabilityResult disbandmentGovernmentAvailability() {
    return ActionAvailabilityResult.allowed();
  }

  private ActionAvailabilityResult resignationAvailability(
      Polity polity, Membership member, GovernmentAssessmentResult assessment) {
    boolean disbandmentUnavailable =
        assessment
            .constitutionalHealth()
            .diagnostics()
            .contains(ConstitutionalHealthDiagnostic.DISBANDMENT_PATH_UNAVAILABLE);
    if (governmentAssessments.activeMemberCount(member.getPolityId()) <= 1
        && disbandmentUnavailable) {
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
}
