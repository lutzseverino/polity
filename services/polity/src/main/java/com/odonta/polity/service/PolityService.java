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
import com.odonta.polity.model.Procedure;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
  public PolityResult create(AuthenticatedUser founder, @Valid CreatePolityInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
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
                ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackTitle(),
                ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackBody(),
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
            .findByConstitutionVersionIdAndCode(constitution.getId(), Office.STEWARD)
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
            Map.of(
                "polityName",
                input.name(),
                "setupPreset",
                setupPreset.name(),
                "pace",
                pace.name())),
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
            Map.of(
                "memberName",
                membership.getDisplayName(),
                "officeName",
                steward.getName(),
                "termLengthDays",
                steward.getTermLengthDays(),
                "constitutionName",
                ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackTitle())),
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
            Map.of(
                "constitutionName",
                ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackTitle(),
                "constitutionBody",
                ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackBody())),
        now);
    return getSummary(polity.getId());
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
        institutions.findByConstitutionVersionId(constitution.getId()).stream()
            .map(this::institutionResult)
            .sorted(java.util.Comparator.comparing(result -> result.kind().name()))
            .toList(),
        procedures.findByConstitutionVersionId(constitution.getId()).stream()
            .map(this::procedureResult)
            .sorted(java.util.Comparator.comparing(ConstitutionProcedureResult::code))
            .toList(),
        offices.findByConstitutionVersionIdOrderByName(constitution.getId()).stream()
            .map(this::officeResult)
            .toList(),
        powers.findByConstitutionVersionId(constitution.getId()).stream()
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
          unavailable);
    }
    Membership member =
        memberships
            .findByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
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
          unavailable);
    }
    ConstitutionVersion constitution = constitution(polityId);
    return new PolityActionAvailabilityResult(
        invitationAvailability(member, constitution),
        fullGovernmentAvailability(member, constitution, PowerCode.INTRODUCE_MOTION),
        fullGovernmentAvailability(member, constitution, PowerCode.INTRODUCE_OFFICE_ELECTION),
        fullGovernmentAvailability(member, constitution, PowerCode.INTRODUCE_SANCTION),
        fullGovernmentAvailability(member, constitution, PowerCode.INTRODUCE_APPEAL),
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
        projection.getCreatedAt());
  }

  ConstitutionVersion constitution(UUID polityId) {
    return constitutions
        .findByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED)
        .orElseThrow(
            () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
  }

  private Polity polity(UUID polityId) {
    return polities
        .findById(polityId)
        .orElseThrow(() -> ApiException.notFound("polity_not_found", "Polity not found."));
  }

  Jurisdiction jurisdiction(UUID polityId) {
    return jurisdictions
        .findByPolityIdAndKind(polityId, JurisdictionKind.ROOT)
        .orElseThrow(
            () -> ApiException.notFound("jurisdiction_not_found", "Jurisdiction not found."));
  }

  Institution institution(UUID polityId) {
    return institution(polityId, constitution(polityId));
  }

  Institution institution(UUID polityId, ConstitutionVersion constitution) {
    return institutions
        .findByPolityIdAndConstitutionVersionIdAndKind(
            polityId, constitution.getId(), InstitutionKind.ASSEMBLY)
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
        officeTerms.findByPolityIdAndOfficeCodeAndStatusAndAssignedByMotionIdIsNull(
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
        && polity.getFounderId().equals(member.getUserId());
  }

  private long activeMemberCount(UUID polityId) {
    return memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE);
  }

  private long standingMemberCount(UUID polityId) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    return memberships
        .findByPolityIdAndStatusOrderByAdmittedAtAsc(polityId, MembershipStatus.ACTIVE)
        .stream()
        .filter(member -> membershipService.hasPoliticalStanding(member, now))
        .count();
  }

  private ConstitutionInstitutionResult institutionResult(Institution institution) {
    return new ConstitutionInstitutionResult(
        institution.getId(),
        institution.getName(),
        institution.getNameKey(),
        institution.getKind());
  }

  private ConstitutionProcedureResult procedureResult(Procedure procedure) {
    return new ConstitutionProcedureResult(
        procedure.getId(),
        procedure.getCode(),
        procedure.getName(),
        procedure.getNameKey(),
        procedure.getQuorumNumerator(),
        procedure.getQuorumDenominator(),
        procedure.getThreshold(),
        procedure.getElectorate(),
        procedure.getElectorateOfficeCode(),
        procedure.getMinimumNoticeHours(),
        procedure.getVotingPeriodHours(),
        procedure.getEffectType());
  }

  private OfficeResult officeResult(Office office) {
    return new OfficeResult(
        office.getId(),
        office.getCode(),
        office.getName(),
        office.getDescription(),
        office.getNameKey(),
        office.getDescriptionKey(),
        office.getTermLengthDays());
  }

  private ConstitutionPowerResult powerResult(ConstitutionalPower power) {
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

  private ActionAvailabilityResult disbandmentAvailability(
      Membership member, ConstitutionVersion constitution) {
    ActionAvailabilityResult authorityResult =
        authorityAvailability(member, constitution, PowerCode.INTRODUCE_DISBANDMENT);
    if (!authorityResult.available()) {
      return authorityResult;
    }
    return activeMemberCount(member.getPolityId()) >= MINIMUM_FULL_GOVERNMENT_MEMBERS
            && standingMemberCount(member.getPolityId()) < MINIMUM_FULL_GOVERNMENT_MEMBERS
        ? ActionAvailabilityResult.blocked("polity_provisional")
        : ActionAvailabilityResult.allowed();
  }

  private ActionAvailabilityResult authorityAvailability(
      Membership member, ConstitutionVersion constitution, PowerCode powerCode) {
    try {
      return authority.allows(member, constitution, powerCode)
          ? ActionAvailabilityResult.allowed()
          : ActionAvailabilityResult.blocked("constitutional_authority_missing");
    } catch (ApiException exception) {
      return ActionAvailabilityResult.blocked(exception.code());
    }
  }
}
