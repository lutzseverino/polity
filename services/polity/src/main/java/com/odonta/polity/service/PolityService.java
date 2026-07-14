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
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.input.CreatePolityInput;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityPace;
import com.odonta.polity.model.PolitySetupPreset;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.resolver.GovernmentAssessmentResolver;
import com.odonta.polity.resolver.PolitySummaryResolver;
import com.odonta.polity.result.PageResult;
import com.odonta.polity.result.PolitySummaryResult;
import com.odonta.polity.template.ConstitutionTemplateSeeder;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
  private final BillingEntitlementsClient entitlements;
  private final ConstitutionTemplateSeeder templates;
  private final ConstitutionVersionRepository constitutions;
  private final Grants grants;
  private final GovernmentAssessmentResolver governmentAssessments;
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
  private final PolitySummaryResolver summaries;

  @Transactional
  public void provisionAccount(AuthenticatedUser user) {
    grants.stage(grantPlanner.account(user.authorizationSubject()));
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_CREATE)
  public PolitySummaryResult create(AuthenticatedUser founder, @Valid CreatePolityInput input) {
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

  public PageResult<PolitySummaryResult> list(UUID userId, int page, int size) {
    Page<PolityProjection> projections =
        polities.findAccessibleProjections(
            userId, MembershipStatus.ACTIVE, PolityVisibility.PUBLIC, PageRequest.of(page, size));
    return new PageResult<>(
        summaries.resolveAll(projections.getContent()),
        projections.getNumber(),
        projections.getSize(),
        projections.getTotalElements());
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PolitySummaryResult get(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return getSummary(polityId);
  }

  private String displayName(IdentityUser user) {
    return user.name() == null || user.name().isBlank() ? user.email() : user.name();
  }

  private PolitySummaryResult getSummary(UUID polityId) {
    return summaries.resolve(
        polities
            .findProjectedById(polityId)
            .orElseThrow(() -> ApiException.notFound("polity_not_found", "Polity not found.")));
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
}
