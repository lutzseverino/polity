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
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.mapper.OfficeApplicationMapper;
import com.odonta.polity.mapper.PolityApplicationMapper;
import com.odonta.polity.model.ActionAvailabilityResult;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.CreatePolityInput;
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
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProposalProjection;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionInstitutionProjection;
import com.odonta.polity.repository.ConstitutionJurisdictionProjection;
import com.odonta.polity.repository.ConstitutionPowerProjection;
import com.odonta.polity.repository.ConstitutionProcedureProjection;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProjection;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.resolver.ProcedureElectorateResolver;
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
import org.springframework.test.util.ReflectionTestUtils;

class PolityServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final MembershipService membershipService = mock(MembershipService.class);
  private final ConstitutionalAuthority authority = mock(ConstitutionalAuthority.class);
  private final AppealProposalRepository appealProposals = mock(AppealProposalRepository.class);
  private final AppealRepository appeals = mock(AppealRepository.class);
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final InstitutionRepository institutions = mock(InstitutionRepository.class);
  private final JurisdictionRepository jurisdictions = mock(JurisdictionRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final MotionRepository motions = mock(MotionRepository.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private final Grants grants = mock(Grants.class);
  private final BillingEntitlementsClient entitlements = mock(BillingEntitlementsClient.class);
  private final IdentityUsersClient identityUsers = mock(IdentityUsersClient.class);
  private PolityService service;

  @BeforeEach
  void setUp() {
    service =
        new PolityService(
            Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
            mock(PolityAccessPolicy.class),
            appealProposals,
            appeals,
            entitlements,
            authority,
            mock(ConstitutionTemplateSeeder.class),
            constitutions,
            powers,
            grants,
            identityUsers,
            Mappers.getMapper(PolityApplicationMapper.class),
            institutions,
            jurisdictions,
            membershipService,
            memberships,
            motions,
            Mappers.getMapper(OfficeApplicationMapper.class),
            offices,
            officeTerms,
            mock(OfficialRecordService.class),
            new PolityGrantPlanner(),
            polities,
            new ProcedureElectorateResolver(memberships, membershipService, officeTerms),
            procedures,
            sanctions);
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
    Polity polity = polity(polityId, UUID.randomUUID());
    Membership member = member(polityId, UUID.randomUUID());

    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(jurisdictions.findProjectionsByPolityId(polityId))
        .thenReturn(List.of(projection(ConstitutionJurisdictionProjection.class, jurisdiction)));
    when(institutions.findProjectionsByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(projection(ConstitutionInstitutionProjection.class, institution)));
    when(procedures.findProjectionsByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(projection(ConstitutionProcedureProjection.class, procedure)));
    when(offices.findProjectionsByConstitutionVersionIdOrderByName(constitutionId))
        .thenReturn(List.of(projection(OfficeProjection.class, steward)));
    when(powers.findProjectionsByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(projection(ConstitutionPowerProjection.class, power)));
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(1L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member));
    when(membershipService.hasPoliticalStanding(member, NOW)).thenReturn(true);

    var result = service.getConstitution(polityId, UUID.randomUUID());

    assertThat(result.version()).isEqualTo(1);
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
    assertThat(result.bootstrap().minimumFullGovernmentMembers()).isEqualTo(3);
    assertThat(result.bootstrap().standingMemberCount()).isEqualTo(1);
  }

  @Test
  void actionsExposeAvailabilityReasonCodes() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    stubPower(constitution, PowerCode.ADMIT_MEMBER, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_MOTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_ELECTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_SANCTION, PowerHolderScope.OFFICE, Office.TRIBUNE);
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
    when(authority.allows(member, constitution, PowerCode.ADMIT_MEMBER)).thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_MOTION)).thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_OFFICE_ELECTION))
        .thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_SANCTION)).thenReturn(false);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_APPEAL)).thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW))
        .thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW))
        .thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_AMENDMENT)).thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_DISBANDMENT)).thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.REQUEST_CERTIFICATION)).thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(2L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member));
    when(membershipService.hasPoliticalStanding(member, NOW)).thenReturn(true);
    stubActiveMemberProcedure(constitution, Procedure.DISBANDMENT, EffectType.DISBAND_POLITY);

    var result = service.getActionAvailability(polityId, userId);

    assertThat(result.inviteMembers().available()).isTrue();
    assertThat(result.introduceMotion().available()).isFalse();
    assertThat(result.introduceMotion().reason()).isEqualTo("polity_provisional");
    assertThat(result.introduceSanction().available()).isFalse();
    assertThat(result.introduceSanction().reason()).isEqualTo("constitutional_office_vacant");
    assertThat(result.introduceDisbandment().available()).isTrue();
    assertThat(result.resignMembership().available()).isFalse();
    assertThat(result.resignMembership().reason())
        .isEqualTo("provisional_founder_resignation_unavailable");
  }

  @Test
  void actionAvailabilityChecksProcedureElectoratesForFullGovernmentProcedures() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    Membership second = member(polityId, UUID.randomUUID());
    Membership third = member(polityId, UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
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
    stubOfficeHolderProcedure(
        constitution, Procedure.ORDINARY_RESOLUTION, EffectType.ADOPT_RESOLUTION);
    stubOfficeHolderProcedure(constitution, Procedure.OFFICE_ELECTION, EffectType.ELECT_OFFICE);
    stubOfficeHolderProcedure(
        constitution, Procedure.CONSTITUTION_AMENDMENT, EffectType.AMEND_CONSTITUTION);
    stubActiveMemberProcedure(constitution, Procedure.APPEAL, EffectType.GRANT_APPEAL);
    stubActiveMemberProcedure(
        constitution, Procedure.OFFICE_TERM_REVIEW, EffectType.VACATE_OFFICE_TERM);
    stubActiveMemberProcedure(
        constitution, Procedure.CONSTITUTIONAL_REVIEW, EffectType.VOID_OFFICIAL_ACT);
    stubActiveMemberProcedure(constitution, Procedure.DISBANDMENT, EffectType.DISBAND_POLITY);
    when(authority.allows(
            any(Membership.class), any(ConstitutionVersion.class), any(PowerCode.class)))
        .thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member, second, third));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);
    when(officeTerms.existsByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, Office.MAGISTRATE, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(false);

    var result = service.getActionAvailability(polityId, userId);

    assertThat(result.introduceMotion().available()).isFalse();
    assertThat(result.introduceMotion().reason()).isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceOfficeElection().available()).isFalse();
    assertThat(result.introduceOfficeElection().reason())
        .isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceAmendment().available()).isFalse();
    assertThat(result.introduceAmendment().reason())
        .isEqualTo("procedure_electorate_office_vacant");
  }

  @Test
  void judicialActionsAreBlockedWhenMagistrateElectorateIsVacant() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    Membership second = member(polityId, UUID.randomUUID());
    Membership third = member(polityId, UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    stubPower(constitution, PowerCode.ADMIT_MEMBER, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_MOTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(
        constitution, PowerCode.INTRODUCE_OFFICE_ELECTION, PowerHolderScope.ACTIVE_MEMBER, null);
    stubPower(constitution, PowerCode.INTRODUCE_SANCTION, PowerHolderScope.OFFICE, Office.TRIBUNE);
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
    stubJudicialProcedure(constitution, Procedure.APPEAL);
    stubJudicialProcedure(constitution, Procedure.OFFICE_TERM_REVIEW);
    stubJudicialProcedure(constitution, Procedure.CONSTITUTIONAL_REVIEW);
    stubActiveMemberProcedure(constitution, Procedure.DISBANDMENT, EffectType.DISBAND_POLITY);
    when(authority.allows(
            any(Membership.class), any(ConstitutionVersion.class), any(PowerCode.class)))
        .thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member, second, third));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);
    when(officeTerms.existsByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, Office.MAGISTRATE, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(false);
    when(officeTerms.existsByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, Office.TRIBUNE, OfficeTermStatus.ACTIVE, NOW))
        .thenReturn(true);

    var result = service.getActionAvailability(polityId, userId);

    assertThat(result.introduceSanction().available()).isFalse();
    assertThat(result.introduceSanction().reason()).isEqualTo("appeal_procedure_unavailable");
    assertThat(result.introduceAppeal().available()).isFalse();
    assertThat(result.introduceAppeal().reason()).isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceOfficeTermReview().available()).isFalse();
    assertThat(result.introduceOfficeTermReview().reason())
        .isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceConstitutionalReview().available()).isFalse();
    assertThat(result.introduceConstitutionalReview().reason())
        .isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceDisbandment().available()).isTrue();
    assertThat(result.resignMembership().available()).isTrue();
  }

  @Test
  void suspendedMembersCanSeeOwnAppealAvailability() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    Membership sanctionIntroducer = member(polityId, UUID.randomUUID());
    Membership judge = member(polityId, UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));
    UUID sanctionMotionId = UUID.randomUUID();
    Sanction sanction =
        new Sanction(
            polityId,
            sanctionMotionId,
            member.getId(),
            SanctionType.SUSPENSION,
            "Timeout",
            NOW.minusDays(1),
            NOW.plusDays(3));
    ReflectionTestUtils.setField(sanction, "id", UUID.randomUUID());

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    stubAllActiveMemberPowers(constitution);
    stubActiveMemberProcedure(constitution, Procedure.APPEAL, EffectType.GRANT_APPEAL);
    when(authority.allows(
            any(Membership.class), any(ConstitutionVersion.class), any(PowerCode.class)))
        .thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.INTRODUCE_APPEAL))
        .thenThrow(ApiException.forbidden("political_standing_required", "Standing required."));
    when(authority.allowsOwnAppealIntroduction(member, constitution)).thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member, sanctionIntroducer, judge));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenAnswer(invocation -> invocation.getArgument(0) != member);
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenAnswer(invocation -> !invocation.getArgument(0).equals(member.getId()));
    when(sanctions
            .findProjectionsByPolityIdAndTargetMembershipIdAndStatusAndEndsAtAfterOrderByStartedAtDesc(
                polityId, member.getId(), SanctionStatus.ACTIVE, NOW))
        .thenReturn(List.of(projection(SanctionProjection.class, sanction)));
    when(appealProposals.findProjectionsByPolityIdAndSanctionId(polityId, sanction.getId()))
        .thenReturn(List.of());
    when(motions.findEntityByIdAndPolityId(sanctionMotionId, polityId))
        .thenReturn(
            Optional.of(
                motion(
                    polityId,
                    sanctionMotionId,
                    constitution.getId(),
                    sanctionIntroducer.getId(),
                    NOW.minusHours(1))));

    var result = service.getActionAvailability(polityId, userId);

    assertThat(result.introduceAppeal().available()).isTrue();
  }

  @Test
  void suspendedMembersCanSeeOwnAppealCertificationAvailability() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));
    UUID appealMotionId = UUID.randomUUID();
    AppealProposal proposal =
        new AppealProposal(polityId, appealMotionId, UUID.randomUUID(), member.getId(), "Evidence");

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(constitutions.findEntityById(constitution.getId())).thenReturn(Optional.of(constitution));
    stubAllActiveMemberPowers(constitution);
    when(authority.allows(
            any(Membership.class), any(ConstitutionVersion.class), any(PowerCode.class)))
        .thenReturn(true);
    when(authority.allows(member, constitution, PowerCode.REQUEST_CERTIFICATION))
        .thenThrow(ApiException.forbidden("political_standing_required", "Standing required."));
    when(authority.allowsAppealCertification(member, constitution)).thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(false);
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenReturn(false);
    when(appealProposals.findProjectionsByPolityIdAndAppellantMembershipId(
            polityId, member.getId()))
        .thenReturn(List.of(projection(AppealProposalProjection.class, proposal)));
    when(motions.findEntityByIdAndPolityId(appealMotionId, polityId))
        .thenReturn(
            Optional.of(
                motion(
                    polityId,
                    appealMotionId,
                    constitution.getId(),
                    member.getId(),
                    NOW.minusMinutes(1))));

    var result = service.getActionAvailability(polityId, userId);

    assertThat(result.requestCertification().available()).isTrue();
  }

  @Test
  void disbandmentRemainsAvailableWhenThreeActiveMembersHaveProvisionalStanding() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId);
    Membership second = member(polityId, UUID.randomUUID());
    Membership suspended = member(polityId, UUID.randomUUID());
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW);
    Polity polity = polity(polityId, userId);
    polity.completeBootstrap(NOW.minusDays(1));

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, MembershipStatus.ACTIVE))
        .thenReturn(Optional.of(member));
    when(constitutions.findEntityByPolityIdAndStatus(
            polityId, com.odonta.polity.model.ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
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
    stubActiveMemberProcedure(constitution, Procedure.DISBANDMENT, EffectType.DISBAND_POLITY);
    when(authority.allows(
            any(Membership.class), any(ConstitutionVersion.class), any(PowerCode.class)))
        .thenReturn(true);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(member, second, suspended));
    when(membershipService.hasPoliticalStanding(member, NOW)).thenReturn(true);
    when(membershipService.hasPoliticalStanding(second, NOW)).thenReturn(true);
    when(membershipService.hasPoliticalStanding(suspended, NOW)).thenReturn(false);
    when(membershipService.hasPoliticalStanding(suspended.getId(), NOW)).thenReturn(false);

    var result = service.getActionAvailability(polityId, userId);

    assertThat(result.introduceMotion().available()).isFalse();
    assertThat(result.introduceMotion().reason()).isEqualTo("polity_provisional");
    assertThat(result.introduceDisbandment().available()).isTrue();
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
    when(membershipService.get(magistrateMembershipId)).thenReturn(magistrate);
    when(membershipService.hasPoliticalStanding(magistrate, NOW)).thenReturn(true);

    ActionAvailabilityResult result =
        service.procedureElectorateAvailability(
            polityId, constitution, Procedure.CONSTITUTIONAL_REVIEW);

    assertThat(result.available()).isFalse();
    assertThat(result.reason()).isEqualTo("procedure_electorate_below_minimum");
  }

  @Test
  void requireFullGovernmentRejectsProvisionalPolities() {
    UUID polityId = UUID.randomUUID();
    Membership first = member(polityId, UUID.randomUUID());
    Membership second = member(polityId, UUID.randomUUID());
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(first, second));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);

    assertThatThrownBy(() -> service.requireFullGovernment(polityId))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "This polity needs at least three citizens with political standing before full government motions can be introduced.");
  }

  @Test
  void requireFullGovernmentAllowsThreeStandingMembers() {
    UUID polityId = UUID.randomUUID();
    Membership first = member(polityId, UUID.randomUUID());
    Membership second = member(polityId, UUID.randomUUID());
    Membership third = member(polityId, UUID.randomUUID());
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(first, second, third));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);

    service.requireFullGovernment(polityId);
  }

  @Test
  void provisionalAdmissionAuthorityBelongsOnlyToFounderBeforeFullGovernment() {
    UUID polityId = UUID.randomUUID();
    UUID founderUserId = UUID.randomUUID();
    Membership founder = member(polityId, founderUserId);
    Membership citizen = member(polityId, UUID.randomUUID());
    Polity polity = polity(polityId, founderUserId);

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(founder, citizen));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);
    when(officeTerms
            .existsByPolityIdAndOfficeCodeAndMembershipIdAndStatusAndAssignedByMotionIdIsNull(
                polityId, Office.STEWARD, founder.getId(), OfficeTermStatus.ACTIVE))
        .thenReturn(true);

    assertThat(service.hasProvisionalFounderAdmissionAuthority(founder)).isTrue();
    assertThat(service.hasProvisionalFounderAdmissionAuthority(citizen)).isFalse();
  }

  @Test
  void provisionalAdmissionAuthoritySurvivesExpiredBootstrapStewardTerm() {
    UUID polityId = UUID.randomUUID();
    UUID founderUserId = UUID.randomUUID();
    Membership founder = member(polityId, founderUserId);
    Membership citizen = member(polityId, UUID.randomUUID());
    Polity polity = polity(polityId, founderUserId);

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(founder, citizen));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);

    assertThat(service.hasProvisionalFounderAdmissionAuthority(founder)).isTrue();
  }

  @Test
  void provisionalAdmissionAuthorityEndsAtFullGovernmentSize() {
    UUID polityId = UUID.randomUUID();
    UUID founderUserId = UUID.randomUUID();
    Membership founder = member(polityId, founderUserId);
    Polity polity = polity(polityId, founderUserId);
    Membership second = member(polityId, UUID.randomUUID());
    Membership third = member(polityId, UUID.randomUUID());

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(founder, second, third));
    when(membershipService.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);

    assertThat(service.hasProvisionalFounderAdmissionAuthority(founder)).isFalse();
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

  @Test
  void provisionalAdmissionAuthorityDoesNotReturnAfterBootstrapCompletes() {
    UUID polityId = UUID.randomUUID();
    UUID founderUserId = UUID.randomUUID();
    Membership founder = member(polityId, founderUserId);
    Polity polity = polity(polityId, founderUserId);
    polity.completeBootstrap(NOW.minusDays(1));

    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));

    assertThat(service.hasProvisionalFounderAdmissionAuthority(founder)).isFalse();
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
