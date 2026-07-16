package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.authorization.grant.GrantPlan;
import com.odonta.authorization.grant.Grants;
import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.billing.client.BillingEntitlement;
import com.odonta.billing.client.BillingEntitlementStatus;
import com.odonta.billing.client.BillingEntitlementsClient;
import com.odonta.common.api.ApiException;
import com.odonta.identity.client.IdentityUsersClient;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.input.CreatePolityInput;
import com.odonta.polity.mapper.ConstitutionApplicationMapper;
import com.odonta.polity.mapper.ConstitutionalPowerApplicationMapper;
import com.odonta.polity.mapper.InstitutionApplicationMapper;
import com.odonta.polity.mapper.OfficeApplicationMapper;
import com.odonta.polity.mapper.ProcedureApplicationMapper;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.InstitutionTemplateKey;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.ProcedureTemplateKey;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerProjection;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionProjection;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureProjection;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.resolver.GovernmentAssessmentResolver;
import com.odonta.polity.resolver.PolitySummaryResolver;
import com.odonta.polity.resolver.ProcedureElectorateResolver;
import com.odonta.polity.result.ActionAvailabilityResult;
import com.odonta.polity.result.ActionUnavailableReason;
import com.odonta.polity.template.ConstitutionTemplateSeeder;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class PolityServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final MembershipService membershipService = mock(MembershipService.class);
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final InstitutionRepository institutions = mock(InstitutionRepository.class);
  private final JurisdictionRepository jurisdictions = mock(JurisdictionRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final Grants grants = mock(Grants.class);
  private final BillingEntitlementsClient entitlements = mock(BillingEntitlementsClient.class);
  private final IdentityUsersClient identityUsers = mock(IdentityUsersClient.class);
  private GovernmentAssessmentResolver governmentAssessments;
  private PolityService service;
  private final PolitySummaryResolver summaries = mock(PolitySummaryResolver.class);

  @BeforeEach
  void setUp() {
    ProcedureElectorateResolver procedureElectorates =
        new ProcedureElectorateResolver(memberships, membershipService, officeTerms);
    governmentAssessments =
        new GovernmentAssessmentResolver(
            Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
            powers,
            memberships,
            membershipService,
            offices,
            officeTerms,
            procedureElectorates,
            procedures);
    service =
        new PolityService(
            Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
            access,
            entitlements,
            mock(ConstitutionTemplateSeeder.class),
            constitutions,
            grants,
            governmentAssessments,
            identityUsers,
            institutions,
            jurisdictions,
            membershipService,
            memberships,
            offices,
            officeTerms,
            mock(OfficialRecordService.class),
            new PolityGrantPlanner(),
            polities,
            summaries);
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenReturn(true);
  }

  @Test
  void provisionAccountStagesCreationAuthority() {
    AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "subject-1", "Citizen");
    ArgumentCaptor<GrantPlan> plan = ArgumentCaptor.forClass(GrantPlan.class);

    service.provisionAccount(user);

    verify(grants).stage(plan.capture());
    assertThat(plan.getValue().resourceGrants()).isEmpty();
    assertThat(plan.getValue().authorityGrants())
        .singleElement()
        .satisfies(
            grant -> {
              assertThat(grant.resourceServerClientId()).isEqualTo(PolityPermissions.CLIENT_ID);
              assertThat(grant.subject()).isEqualTo("subject-1");
              assertThat(grant.authorities()).containsExactly(PolityPermissions.POLITY_CREATE);
            });
  }

  @Test
  void listTrimsThePolityQueryBeforeApplyingPagination() {
    UUID userId = UUID.randomUUID();
    PageRequest pageRequest = PageRequest.of(1, 25);
    when(polities.findAccessibleProjections(
            userId, MembershipStatus.ACTIVE, PolityVisibility.PUBLIC, "assembly", pageRequest))
        .thenReturn(Page.<PolityProjection>empty(pageRequest));
    when(summaries.resolveAll(List.of())).thenReturn(List.of());

    service.list(userId, "  assembly  ", 1, 25);

    verify(polities)
        .findAccessibleProjections(
            userId, MembershipStatus.ACTIVE, PolityVisibility.PUBLIC, "assembly", pageRequest);
  }

  @Test
  void listTreatsABlankPolityQueryAsAbsent() {
    UUID userId = UUID.randomUUID();
    PageRequest pageRequest = PageRequest.of(0, 50);
    when(polities.findAccessibleProjections(
            userId, MembershipStatus.ACTIVE, PolityVisibility.PUBLIC, null, pageRequest))
        .thenReturn(Page.<PolityProjection>empty(pageRequest));
    when(summaries.resolveAll(List.of())).thenReturn(List.of());

    service.list(userId, " \t ", 0, 50);

    verify(polities)
        .findAccessibleProjections(
            userId, MembershipStatus.ACTIVE, PolityVisibility.PUBLIC, null, pageRequest);
  }

  @Test
  void createRejectsPrivatePolityWhenEntitlementLimitIsReached() {
    UUID founderId = UUID.randomUUID();
    AuthenticatedUser founder = new AuthenticatedUser(founderId, "subject-1", "Founder");
    CreatePolityInput input =
        new CreatePolityInput("Private Room", PolityVisibility.PRIVATE, null, null);

    when(entitlements.require(founderId, PolityResources.PRODUCT)).thenReturn(entitlement(1));
    when(polities.countByFounderIdAndVisibilityAndStatus(
            founderId, PolityVisibility.PRIVATE, PolityStatus.ACTIVE))
        .thenReturn(1L);

    assertThatThrownBy(() -> service.create(founder, input))
        .isInstanceOf(ApiException.class)
        .hasMessage("Private polity entitlement limit has been reached.");

    verify(polities).lockFounderPrivatePolityQuota(founderId);
    verify(identityUsers, never()).get(any());
    verify(polities, never()).saveAndFlush(any());
    verify(grants, never()).stage(any());
  }

  @Test
  void createAllowsPrivatePolityWhenEntitlementHasCapacity() {
    UUID founderId = UUID.randomUUID();
    AuthenticatedUser founder = new AuthenticatedUser(founderId, "subject-1", "Founder");
    CreatePolityInput input =
        new CreatePolityInput("Private Room", PolityVisibility.PRIVATE, null, null);

    when(entitlements.require(founderId, PolityResources.PRODUCT)).thenReturn(entitlement(2));
    when(polities.countByFounderIdAndVisibilityAndStatus(
            founderId, PolityVisibility.PRIVATE, PolityStatus.ACTIVE))
        .thenReturn(1L);
    when(identityUsers.get(founderId))
        .thenThrow(ApiException.of(500, "stop", "Stop after billing branch."));

    assertThatThrownBy(() -> service.create(founder, input))
        .isInstanceOf(ApiException.class)
        .hasMessage("Stop after billing branch.");

    verify(polities).lockFounderPrivatePolityQuota(founderId);
    verify(identityUsers).get(founderId);
    verify(polities, never()).saveAndFlush(any());
  }

  @Test
  void createChecksBillingOnlyForPrivatePolities() {
    AuthenticatedUser founder = new AuthenticatedUser(UUID.randomUUID(), "subject-1", "Founder");
    CreatePolityInput input =
        new CreatePolityInput("Public Square", PolityVisibility.PUBLIC, null, null);

    when(identityUsers.get(founder.id()))
        .thenThrow(ApiException.of(500, "stop", "Stop after billing branch."));

    assertThatThrownBy(() -> service.create(founder, input))
        .isInstanceOf(ApiException.class)
        .hasMessage("Stop after billing branch.");

    verify(entitlements, never()).require(any(), any());
    verify(polities, never()).lockFounderPrivatePolityQuota(any());
  }

  @Test
  void constitutionResultExposesStructuredCharter() {
    UUID polityId = UUID.randomUUID();
    UUID constitutionId = UUID.randomUUID();
    UUID jurisdictionId = UUID.randomUUID();
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", jurisdictionId);
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW.minusDays(1));
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    Institution institution =
        new Institution(
            polityId,
            jurisdictionId,
            constitutionId,
            InstitutionTemplateKey.CITIZENS_ASSEMBLY.storedName(),
            InstitutionTemplateKey.CITIZENS_ASSEMBLY,
            InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(institution, "id", UUID.randomUUID());
    Office steward =
        new Office(
            polityId,
            constitutionId,
            jurisdictionId,
            Office.STEWARD,
            "Steward",
            "Coordinates proceedings.",
            14);
    Procedure procedure =
        new Procedure(
            polityId,
            constitutionId,
            institution.getId(),
            Procedure.ORDINARY_RESOLUTION,
            ProcedureTemplateKey.ORDINARY_RESOLUTION.storedName(),
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            0,
            48,
            EffectType.ADOPT_RESOLUTION);
    ConstitutionalPower power =
        new ConstitutionalPower(
            polityId,
            constitutionId,
            PowerCode.INTRODUCE_MOTION,
            "Introduce motions",
            PowerHolderScope.ACTIVE_MEMBER);
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(institutions.findProjectionsByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(projection(InstitutionProjection.class, institution)));
    when(procedures.findProjectionsByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(projection(ProcedureProjection.class, procedure)));
    when(offices.findProjectionsByConstitutionVersionIdOrderByNameAscIdAsc(constitutionId))
        .thenReturn(List.of(projection(OfficeProjection.class, steward)));
    when(powers.findProjectionsByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(projection(ConstitutionalPowerProjection.class, power)));
    ConstitutionService constitutionService =
        new ConstitutionService(
            mock(PolityAccessPolicy.class),
            Mappers.getMapper(ConstitutionApplicationMapper.class),
            constitutions,
            Mappers.getMapper(InstitutionApplicationMapper.class),
            institutions,
            Mappers.getMapper(ProcedureApplicationMapper.class),
            procedures,
            Mappers.getMapper(OfficeApplicationMapper.class),
            offices,
            Mappers.getMapper(ConstitutionalPowerApplicationMapper.class),
            powers);

    var result = constitutionService.get(polityId, UUID.randomUUID());

    assertThat(result.version()).isEqualTo(1);
    assertThat(result.title()).isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle());
    assertThat(result.body()).isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody());
    assertThat(result.institutions())
        .singleElement()
        .satisfies(
            found -> {
              assertThat(found.name())
                  .isEqualTo(InstitutionTemplateKey.CITIZENS_ASSEMBLY.storedName());
              assertThat(found.nameKey())
                  .isEqualTo(InstitutionTemplateKey.CITIZENS_ASSEMBLY.nameKey());
            });
    assertThat(result.procedures())
        .singleElement()
        .extracting("code")
        .isEqualTo(Procedure.ORDINARY_RESOLUTION);
    assertThat(result.offices()).singleElement().extracting("code").isEqualTo(Office.STEWARD);
    assertThat(result.powers())
        .singleElement()
        .extracting("code")
        .isEqualTo(PowerCode.INTRODUCE_MOTION);
  }

  @Test
  void procedureAvailabilityRequiresMinimumOfficeHolderElectorate() {
    UUID polityId = UUID.randomUUID();
    UUID constitutionId = UUID.randomUUID();
    UUID magistrateMembershipId = UUID.randomUUID();
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Structured Charter", "Rules", NOW);
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    Procedure procedure =
        new Procedure(
            polityId,
            constitutionId,
            UUID.randomUUID(),
            Procedure.CONSTITUTIONAL_REVIEW,
            "Constitutional review",
            (ProcedureTemplateKey) null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            2,
            0,
            24,
            EffectType.VOID_OFFICIAL_ACT);
    OfficeTerm term =
        new OfficeTerm(
            polityId,
            UUID.randomUUID(),
            Office.MAGISTRATE,
            magistrateMembershipId,
            NOW.minusDays(1),
            NOW.plusDays(14));
    Membership magistrate = member(polityId, UUID.randomUUID());
    ReflectionTestUtils.setField(magistrate, "id", magistrateMembershipId);

    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitutionId, Procedure.CONSTITUTIONAL_REVIEW))
        .thenReturn(Optional.of(procedure));
    when(officeTerms.existsByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, Office.MAGISTRATE, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(true);
    when(officeTerms.findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
            polityId, Office.MAGISTRATE, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(List.of(term));
    when(memberships.findEntityById(magistrateMembershipId)).thenReturn(Optional.of(magistrate));
    when(membershipService.hasPoliticalStanding(magistrate, NOW)).thenReturn(true);

    ActionAvailabilityResult result =
        governmentAssessments.procedureAvailability(
            polityId, constitution, Procedure.CONSTITUTIONAL_REVIEW);

    assertThat(result.available()).isFalse();
    assertThat(result.reason())
        .isEqualTo(ActionUnavailableReason.PROCEDURE_ELECTORATE_BELOW_MINIMUM);
  }

  @Test
  void completingBootstrapEndsInitialStewardTermAtFullGovernmentSize() {
    UUID polityId = UUID.randomUUID();
    Membership first = member(polityId, UUID.randomUUID());
    Membership second = member(polityId, UUID.randomUUID());
    Membership third = member(polityId, UUID.randomUUID());
    OfficeTerm bootstrapTerm =
        new OfficeTerm(
            polityId,
            UUID.randomUUID(),
            Office.STEWARD,
            first.getId(),
            NOW.minusDays(1),
            NOW.plusDays(14));

    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(first, second, third));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);
    when(polities.findEntityById(polityId))
        .thenReturn(Optional.of(polity(polityId, first.getUserId())));
    when(officeTerms.findEntitiesByPolityIdAndOfficeCodeAndStatusAndAssignedByMotionIdIsNull(
            polityId, Office.STEWARD, OfficeTermStatus.ACTIVE))
        .thenReturn(List.of(bootstrapTerm));

    service.completeBootstrapIfReady(polityId, NOW);

    assertThat(bootstrapTerm.getStatus()).isEqualTo(OfficeTermStatus.ENDED);
    assertThat(bootstrapTerm.getEndedAt()).isEqualTo(NOW);
    verify(polities).saveAndFlush(any(Polity.class));
    verify(officeTerms).saveAllAndFlush(List.of(bootstrapTerm));
  }

  @Test
  void completingBootstrapLeavesStewardTermBeforeFullGovernmentSize() {
    UUID polityId = UUID.randomUUID();
    Membership first = member(polityId, UUID.randomUUID());
    Membership second = member(polityId, UUID.randomUUID());

    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(first, second));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);
    when(polities.findEntityById(polityId))
        .thenReturn(Optional.of(polity(polityId, first.getUserId())));

    service.completeBootstrapIfReady(polityId, NOW);

    verify(officeTerms, never())
        .findEntitiesByPolityIdAndOfficeCodeAndStatusAndAssignedByMotionIdIsNull(
            polityId, Office.STEWARD, OfficeTermStatus.ACTIVE);
    verify(officeTerms, never()).saveAllAndFlush(any());
  }

  private Membership member(UUID polityId, UUID userId) {
    Membership member =
        new Membership(
            polityId, userId, "subject:" + userId, "founder@example.com", "Founder", NOW, null);
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private Polity polity(UUID polityId, UUID founderUserId) {
    Polity polity = new Polity("Pocket Republic", PolityVisibility.PRIVATE, founderUserId);
    ReflectionTestUtils.setField(polity, "id", polityId);
    return polity;
  }

  private BillingEntitlement entitlement(Integer tenantLimit) {
    return new BillingEntitlement(
        UUID.randomUUID(),
        UUID.randomUUID(),
        PolityResources.PRODUCT,
        BillingEntitlementStatus.ACTIVE,
        tenantLimit,
        null,
        null,
        null,
        NOW,
        NOW);
  }

  private void stubPower(
      ConstitutionVersion constitution,
      PowerCode code,
      PowerHolderScope holderScope,
      String holderOfficeCode) {
    ConstitutionalPower power =
        holderScope == PowerHolderScope.OFFICE
            ? new ConstitutionalPower(
                constitution.getPolityId(),
                constitution.getId(),
                code,
                code.name(),
                holderOfficeCode)
            : new ConstitutionalPower(
                constitution.getPolityId(), constitution.getId(), code, code.name(), holderScope);
    when(powers.findEntityByConstitutionVersionIdAndCode(constitution.getId(), code))
        .thenReturn(Optional.of(power));
  }

  private void stubAllActiveMemberPowers(ConstitutionVersion constitution) {
    stubPower(constitution, PowerCode.ADMIT_MEMBER, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_MOTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_ELECTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_SANCTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_APPEAL, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution,
        PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW,
        PowerHolderScope.ACTIVE_MEMBER,
        null);
    stubPower(constitution, PowerCode.INTRODUCE_AMENDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_DISBANDMENT, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.REQUEST_CERTIFICATION, PowerHolderScope.ACTIVE_MEMBER, null);
  }

  private void stubJudicialProcedure(ConstitutionVersion constitution, String procedureCode) {
    stubOfficeHolderProcedure(constitution, procedureCode, EffectType.GRANT_APPEAL);
  }

  private void stubOfficeHolderProcedure(
      ConstitutionVersion constitution, String procedureCode, EffectType effectType) {
    Procedure procedure =
        procedure(
            constitution,
            procedureCode,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            2,
            effectType);
    when(procedures.findEntityByConstitutionVersionIdAndCode(constitution.getId(), procedureCode))
        .thenReturn(Optional.of(procedure));
  }

  private void stubActiveMemberProcedure(
      ConstitutionVersion constitution, String procedureCode, EffectType effectType) {
    Procedure procedure =
        procedure(
            constitution, procedureCode, ProcedureElectorate.ACTIVE_MEMBERS, null, 1, effectType);
    when(procedures.findEntityByConstitutionVersionIdAndCode(constitution.getId(), procedureCode))
        .thenReturn(Optional.of(procedure));
  }

  private Procedure procedure(
      ConstitutionVersion constitution,
      String procedureCode,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      int minimumElectorCount,
      EffectType effectType) {
    return new Procedure(
        constitution.getPolityId(),
        constitution.getId(),
        UUID.randomUUID(),
        procedureCode,
        procedureCode,
        (ProcedureTemplateKey) null,
        1,
        2,
        VotingThreshold.SIMPLE_MAJORITY_CAST,
        electorate,
        electorateOfficeCode,
        minimumElectorCount,
        0,
        24,
        effectType);
  }

  private Motion motion(
      UUID polityId,
      UUID motionId,
      UUID constitutionVersionId,
      UUID introducedBy,
      OffsetDateTime certificationOpensAt) {
    Motion motion =
        new Motion(
            polityId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            constitutionVersionId,
            UUID.randomUUID(),
            introducedBy,
            "Appeal",
            "Review the sanction.",
            EffectType.GRANT_APPEAL,
            NOW.minusDays(2),
            NOW.minusDays(1),
            NOW.minusHours(1),
            certificationOpensAt);
    ReflectionTestUtils.setField(motion, "id", motionId);
    return motion;
  }

  private static <T> T projection(Class<T> type, Object source) {
    Object proxy =
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (ignored, method, args) -> {
              if (method.getDeclaringClass() == Object.class) {
                return method.invoke(source, args);
              }
              return source.getClass().getMethod(method.getName()).invoke(source);
            });
    return type.cast(proxy);
  }
}
