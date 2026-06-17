package com.odonta.polity.service;

import com.odonta.authorization.grant.Grants;
import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.identity.client.IdentityUser;
import com.odonta.identity.client.IdentityUsersClient;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.mapper.PolityApplicationMapper;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.CreatePolityInput;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipResult;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityResult;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
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
  private final ConstitutionTemplateService templates;
  private final ConstitutionVersionRepository constitutions;
  private final Grants grants;
  private final IdentityUsersClient identityUsers;
  private final InstitutionRepository institutions;
  private final JurisdictionRepository jurisdictions;
  private final MembershipReader membershipReader;
  private final MembershipRepository memberships;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficialRecordWriter record;
  private final PolityApplicationMapper mapper;
  private final PolityGrantPlanner grantPlanner;
  private final PolityRepository polities;

  @Transactional
  public PolityResult create(AuthenticatedUser founder, @Valid CreatePolityInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    IdentityUser identity = identityUsers.get(founder.id());
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
                "Starter Constitution",
                ConstitutionTemplateService.STARTER_CONSTITUTION,
                now));
    templates.establishStarterRepublic(jurisdiction, constitution);
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
    grants.stage(grantPlanner.founder(founder.authorizationSubject(), polity.getId()));
    record.append(
        polity.getId(),
        jurisdiction.getId(),
        constitution.getId(),
        membership.getId(),
        OfficialRecordType.POLITY_FOUNDED,
        polity.getId(),
        input.name() + " was founded",
        "The polity was founded with a root jurisdiction and citizens' assembly.",
        now);
    record.append(
        polity.getId(),
        jurisdiction.getId(),
        constitution.getId(),
        membership.getId(),
        OfficialRecordType.OFFICE_ASSIGNED,
        stewardTerm.getId(),
        membership.getDisplayName() + " assigned as " + steward.getName(),
        "The founding citizen received the initial Steward term under the Starter Constitution.",
        now);
    record.append(
        polity.getId(),
        jurisdiction.getId(),
        constitution.getId(),
        membership.getId(),
        OfficialRecordType.CONSTITUTION_RATIFIED,
        constitution.getId(),
        "Starter Constitution ratified",
        ConstitutionTemplateService.STARTER_CONSTITUTION,
        now);
    return getProjection(polity.getId());
  }

  public List<PolityResult> list(UUID userId) {
    return mapper.toResults(
        polities.findAccessibleProjections(
            userId,
            MembershipStatus.ACTIVE,
            PolityVisibility.PUBLIC,
            ConstitutionStatus.RATIFIED,
            JurisdictionKind.ROOT,
            InstitutionKind.ASSEMBLY));
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PolityResult get(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return getProjection(polityId);
  }

  @PreAuthorize(PolityPermissions.HAS_POLITY_READ)
  public List<MembershipResult> members(UUID polityId, UUID userId) {
    membershipReader.active(polityId, userId);
    return mapper.toMemberResults(
        memberships.findProjectionsByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE));
  }

  private String displayName(IdentityUser user) {
    return user.name() == null || user.name().isBlank() ? user.email() : user.name();
  }

  private PolityResult getProjection(UUID polityId) {
    return mapper.toResult(
        polities
            .findProjectedById(
                polityId,
                ConstitutionStatus.RATIFIED,
                JurisdictionKind.ROOT,
                InstitutionKind.ASSEMBLY)
            .orElseThrow(() -> ApiException.notFound("polity_not_found", "Polity not found.")));
  }

  ConstitutionVersion constitution(UUID polityId) {
    return constitutions
        .findByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED)
        .orElseThrow(
            () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
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
}
