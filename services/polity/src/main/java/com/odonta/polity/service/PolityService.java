package com.odonta.polity.service;

import com.odonta.authorization.grant.Grants;
import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.identity.client.IdentityUser;
import com.odonta.identity.client.IdentityUsersClient;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.model.ActionAvailabilityResult;
import com.odonta.polity.model.ConstitutionBootstrapResult;
import com.odonta.polity.model.ConstitutionInstitutionResult;
import com.odonta.polity.model.ConstitutionJurisdictionResult;
import com.odonta.polity.model.ConstitutionPowerResult;
import com.odonta.polity.model.ConstitutionProcedureResult;
import com.odonta.polity.model.ConstitutionResult;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.CreatePolityInput;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeResult;
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
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.ConstitutionInstitutionProjection;
import com.odonta.polity.repository.ConstitutionJurisdictionProjection;
import com.odonta.polity.repository.ConstitutionPowerProjection;
import com.odonta.polity.repository.ConstitutionProcedureProjection;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
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
  private static final int MINIMUM_FULL_GOVERNMENT_MEMBERS = 3;

  private final Clock clock;
  private final PolityAccessPolicy access;
  private final ConstitutionalAuthority authority;
  private final ConstitutionTemplateService templates;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalPowerRepository powers;
  private final Grants grants;
  private final IdentityUsersClient identityUsers;
  private final InstitutionRepository institutions;
  private final JurisdictionRepository jurisdictions;
  private final MembershipService membershipService;
  private final MembershipRepository memberships;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficialRecordService officialRecords;
  private final PolityGrantPlanner grantPlanner;
  private final PolityRepository polities;
  private final ProcedureRepository procedures;

  @Transactional
  public void provisionAccount(AuthenticatedUser user) {
    grants.stage(grantPlanner.account(user.authorizationSubject()));
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_PUBLIC_POLITY_CREATE)
  public PolityResult create(AuthenticatedUser founder, @Valid CreatePolityInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    requireSupportedVisibility(input.visibility());
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
    if (setupPreset == PolitySetupPreset.STANDARD_REPUBLIC) {
      templates.establishStarterRepublic(jurisdiction, constitution, pace);
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

  private void requireSupportedVisibility(PolityVisibility visibility) {
    if (visibility == PolityVisibility.PRIVATE) {
      throw ApiException.conflict(
          "private_polities_not_enabled", "Private polities are not available yet.");
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
    return new ConstitutionResult(
        constitution.getId(),
        constitution.getVersion(),
        constitution.getStatus(),
        constitution.getRatifiedAt(),
        jurisdictions.findProjectionsByPolityId(polityId).stream()
            .map(this::jurisdictionResult)
            .sorted(java.util.Comparator.comparing(result -> result.kind().name()))
            .toList(),
        institutions.findProjectionsByConstitutionVersionId(constitution.getId()).stream()
            .map(this::institutionResult)
            .sorted(java.util.Comparator.comparing(result -> result.kind().name()))
            .toList(),
        procedures.findProjectionsByConstitutionVersionId(constitution.getId()).stream()
            .map(this::procedureResult)
            .sorted(java.util.Comparator.comparing(ConstitutionProcedureResult::code))
            .toList(),
        offices.findProjectionsByConstitutionVersionIdOrderByName(constitution.getId()).stream()
            .map(this::officeResult)
            .toList(),
        powers.findProjectionsByConstitutionVersionId(constitution.getId()).stream()
            .map(this::powerResult)
            .sorted(java.util.Comparator.comparing(result -> result.code().name()))
            .toList(),
        new ConstitutionBootstrapResult(
            polity.isBootstrapComplete(),
            polity.getBootstrapCompletedAt(),
            MINIMUM_FULL_GOVERNMENT_MEMBERS,
            activeMemberCount(polityId),
            standingMemberCount(polityId)));
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PolityActionAvailabilityResult getActionAvailability(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    Polity polity = polity(polityId);
    if (polity.isDisbanded()) {
      ActionAvailabilityResult unavailable = ActionAvailabilityResult.blocked("polity_disbanded");
      return new PolityActionAvailabilityResult(
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
    ConstitutionVersion constitution = constitution(polityId);
    return new PolityActionAvailabilityResult(
        invitationAvailability(member, constitution),
        fullGovernmentAvailability(member, constitution, PowerCode.INTRODUCE_MOTION),
        fullGovernmentAvailability(member, constitution, PowerCode.INTRODUCE_OFFICE_ELECTION),
        fullGovernmentAvailability(member, constitution, PowerCode.INTRODUCE_SANCTION),
        fullGovernmentProcedureAvailability(
            member, constitution, PowerCode.INTRODUCE_APPEAL, Procedure.APPEAL),
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
        fullGovernmentAvailability(member, constitution, PowerCode.INTRODUCE_AMENDMENT),
        disbandmentAvailability(member, constitution),
        authorityAvailability(member, constitution, PowerCode.REQUEST_CERTIFICATION));
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
    return new PolityResult(
        projection.getId(),
        projection.getName(),
        projection.getVisibility(),
        projection.getStatus(),
        constitution.getVersion(),
        jurisdiction.getName(),
        institution.getName(),
        institution.getNameKey(),
        projection.getCreatedAt());
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
        .filter(institution -> institution.getKind() == InstitutionKind.ASSEMBLY)
        .findFirst()
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

  void requireFullGovernment(UUID polityId) {
    if (standingMemberCount(polityId) < MINIMUM_FULL_GOVERNMENT_MEMBERS) {
      throw ApiException.conflict(
          "polity_provisional",
          "This polity needs at least three citizens with political standing before full government motions can be introduced.");
    }
  }

  void requireDisbandmentGovernment(UUID polityId) {
    long activeMembers = activeMemberCount(polityId);
    if (activeMembers >= MINIMUM_FULL_GOVERNMENT_MEMBERS) {
      requireFullGovernment(polityId);
    }
  }

  void completeBootstrapIfReady(UUID polityId, OffsetDateTime now) {
    Polity polity = polity(polityId);
    if (polity.isBootstrapComplete()
        || standingMemberCount(polityId) < MINIMUM_FULL_GOVERNMENT_MEMBERS) {
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
    return standingMemberCount(member.getPolityId()) < MINIMUM_FULL_GOVERNMENT_MEMBERS
        && polity.getFounderId().equals(member.getUserId())
        && officeTerms
            .existsByPolityIdAndOfficeCodeAndMembershipIdAndStatusAndAssignedByMotionIdIsNull(
                member.getPolityId(), Office.STEWARD, member.getId(), OfficeTermStatus.ACTIVE);
  }

  private long activeMemberCount(UUID polityId) {
    return memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE);
  }

  private long standingMemberCount(UUID polityId) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    return memberships
        .findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(polityId, MembershipStatus.ACTIVE)
        .stream()
        .filter(member -> membershipService.hasPoliticalStanding(member, now))
        .count();
  }

  private ConstitutionJurisdictionResult jurisdictionResult(
      ConstitutionJurisdictionProjection jurisdiction) {
    return new ConstitutionJurisdictionResult(
        jurisdiction.getId(), jurisdiction.getName(), jurisdiction.getKind());
  }

  private ConstitutionInstitutionResult institutionResult(
      ConstitutionInstitutionProjection institution) {
    return new ConstitutionInstitutionResult(
        institution.getId(),
        institution.getJurisdictionId(),
        institution.getName(),
        institution.getNameKey(),
        institution.getKind());
  }

  private ConstitutionProcedureResult procedureResult(ConstitutionProcedureProjection procedure) {
    return new ConstitutionProcedureResult(
        procedure.getId(),
        procedure.getInstitutionId(),
        procedure.getCode(),
        procedure.getName(),
        procedure.getNameKey(),
        procedure.getQuorumNumerator(),
        procedure.getQuorumDenominator(),
        procedure.getThreshold(),
        procedure.getElectorate(),
        procedure.getElectorateOfficeCode(),
        procedure.getMinimumElectorCount(),
        procedure.getMinimumNoticeHours(),
        procedure.getVotingPeriodHours(),
        procedure.getEffectType());
  }

  private OfficeResult officeResult(OfficeProjection office) {
    return new OfficeResult(
        office.getId(),
        office.getJurisdictionId(),
        office.getCode(),
        office.getName(),
        office.getDescription(),
        office.getNameKey(),
        office.getDescriptionKey(),
        office.getTermLengthDays(),
        office.getSeatCount());
  }

  private ConstitutionPowerResult powerResult(ConstitutionPowerProjection power) {
    return new ConstitutionPowerResult(
        power.getCode(),
        power.getName(),
        power.getNameKey(),
        power.getHolderScope(),
        power.getHolderOfficeCode());
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

  private ActionAvailabilityResult fullGovernmentAvailability(
      Membership member, ConstitutionVersion constitution, PowerCode powerCode) {
    ActionAvailabilityResult authorityResult =
        authorityAvailability(member, constitution, powerCode);
    if (!authorityResult.available()) {
      return authorityResult;
    }
    return standingMemberCount(member.getPolityId()) < MINIMUM_FULL_GOVERNMENT_MEMBERS
        ? ActionAvailabilityResult.blocked("polity_provisional")
        : ActionAvailabilityResult.allowed();
  }

  private ActionAvailabilityResult fullGovernmentProcedureAvailability(
      Membership member,
      ConstitutionVersion constitution,
      PowerCode powerCode,
      String procedureCode) {
    ActionAvailabilityResult authorityResult =
        fullGovernmentAvailability(member, constitution, powerCode);
    if (!authorityResult.available()) {
      return authorityResult;
    }
    return procedureElectorateAvailability(member.getPolityId(), constitution, procedureCode);
  }

  ActionAvailabilityResult procedureElectorateAvailability(
      UUID polityId, ConstitutionVersion constitution, String procedureCode) {
    try {
      Procedure procedure =
          procedures
              .findEntityByConstitutionVersionIdAndCode(constitution.getId(), procedureCode)
              .orElseThrow(
                  () ->
                      ApiException.forbidden(
                          "procedure_missing",
                          "The governing constitution does not define this procedure."));
      if (procedure.getElectorate() == ProcedureElectorate.OFFICE_HOLDERS
          && !officeTerms.existsByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
              polityId,
              procedure.getElectorateOfficeCode(),
              OfficeTermStatus.ACTIVE,
              OffsetDateTime.now(clock))) {
        return ActionAvailabilityResult.blocked("procedure_electorate_office_vacant");
      }
      if (eligibleElectorCount(polityId, procedure) < procedure.getMinimumElectorCount()) {
        return ActionAvailabilityResult.blocked("procedure_electorate_below_minimum");
      }
      return ActionAvailabilityResult.allowed();
    } catch (ApiException exception) {
      return ActionAvailabilityResult.blocked(exception.code());
    }
  }

  private long eligibleElectorCount(UUID polityId, Procedure procedure) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    if (procedure.getElectorate() == ProcedureElectorate.OFFICE_HOLDERS) {
      return officeTerms
          .findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
              polityId, procedure.getElectorateOfficeCode(), OfficeTermStatus.ACTIVE, now)
          .stream()
          .map(OfficeTerm::getMembershipId)
          .map(membershipService::get)
          .filter(member -> membershipService.hasPoliticalStanding(member, now))
          .count();
    }
    return memberships
        .findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(polityId, MembershipStatus.ACTIVE)
        .stream()
        .filter(member -> membershipService.hasPoliticalStanding(member, now))
        .count();
  }

  private ActionAvailabilityResult disbandmentAvailability(
      Membership member, ConstitutionVersion constitution) {
    ActionAvailabilityResult authorityResult =
        authorityAvailability(member, constitution, PowerCode.INTRODUCE_DISBANDMENT);
    if (!authorityResult.available()) {
      return authorityResult;
    }
    return disbandmentSafeguardAvailability(member, constitution);
  }

  ActionAvailabilityResult disbandmentSafeguardAvailability(
      Membership member, ConstitutionVersion constitution) {
    long activeMembers = activeMemberCount(member.getPolityId());
    if (activeMembers < MINIMUM_FULL_GOVERNMENT_MEMBERS) {
      return ActionAvailabilityResult.allowed();
    }
    if (standingMemberCount(member.getPolityId()) < MINIMUM_FULL_GOVERNMENT_MEMBERS) {
      return ActionAvailabilityResult.blocked("polity_provisional");
    }
    ActionAvailabilityResult reviewAvailability =
        procedureElectorateAvailability(
            member.getPolityId(), constitution, Procedure.CONSTITUTIONAL_REVIEW);
    return !reviewAvailability.available()
            && !"procedure_missing".equals(reviewAvailability.reason())
        ? reviewAvailability
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
