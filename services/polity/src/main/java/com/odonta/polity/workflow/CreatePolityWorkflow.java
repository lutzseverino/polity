package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.input.CreatePolityInput;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityPace;
import com.odonta.polity.model.PolitySetupPreset;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.resolver.PolitySummaryResolver;
import com.odonta.polity.result.PolitySummaryResult;
import com.odonta.polity.service.OfficialRecordService;
import com.odonta.polity.template.ConstitutionTemplateSeeder;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.billing.client.BillingEntitlement;
import io.github.lutzseverino.cardo.billing.client.BillingEntitlementsClient;
import io.github.lutzseverino.cardo.common.api.ApiException;
import io.github.lutzseverino.cardo.identity.client.IdentityUser;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
public class CreatePolityWorkflow {
  private final Clock clock;
  private final BillingEntitlementsClient entitlements;
  private final ConstitutionTemplateSeeder templates;
  private final ConstitutionVersionRepository constitutions;
  private final Grants grants;
  private final IdentityUsersClient identityUsers;
  private final JurisdictionRepository jurisdictions;
  private final MembershipRepository memberships;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficialRecordService officialRecords;
  private final PolityGrantPlanner grantPlanner;
  private final PolityRepository polities;
  private final PolitySummaryResolver summaries;

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
            .orElseThrow(PolityResource.OFFICE::notFound);
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
    appendFoundingRecords(
        input,
        setupPreset,
        pace,
        polity,
        jurisdiction,
        constitution,
        membership,
        steward,
        stewardTerm,
        now);
    return summaries.resolve(
        polities.findProjectedById(polity.getId()).orElseThrow(PolityResource.POLITY::notFound));
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

  private String displayName(IdentityUser user) {
    return user.name() == null || user.name().isBlank() ? user.email() : user.name();
  }

  private void appendFoundingRecords(
      CreatePolityInput input,
      PolitySetupPreset setupPreset,
      PolityPace pace,
      Polity polity,
      Jurisdiction jurisdiction,
      ConstitutionVersion constitution,
      Membership membership,
      Office steward,
      OfficeTerm stewardTerm,
      OffsetDateTime now) {
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
  }
}
