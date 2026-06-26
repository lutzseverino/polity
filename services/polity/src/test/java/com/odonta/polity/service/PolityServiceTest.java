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
import com.odonta.common.api.ApiException;
import com.odonta.identity.client.IdentityUsersClient;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.model.ActionAvailabilityResult;
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
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.ProcedureTemplateKey;
import com.odonta.polity.model.VotingThreshold;
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
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class PolityServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final MembershipService membershipService = mock(MembershipService.class);
  private final ConstitutionalAuthority authority = mock(ConstitutionalAuthority.class);
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
  private final IdentityUsersClient identityUsers = mock(IdentityUsersClient.class);
  private PolityService service;

  @BeforeEach
  void setUp() {
    service =
        new PolityService(
            Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
            mock(PolityAccessPolicy.class),
            authority,
            mock(ConstitutionTemplateService.class),
            constitutions,
            powers,
            grants,
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
            procedures);
  }

  @Test
  void provisionAccountStagesPublicCreationAuthority() {
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
              assertThat(grant.authorities())
                  .containsExactly(PolityPermissions.PUBLIC_POLITY_CREATE);
            });
  }

  @Test
  void createRejectsPrivatePolitiesUntilBillingIsEnabled() {
    AuthenticatedUser founder = new AuthenticatedUser(UUID.randomUUID(), "subject-1", "Founder");
    CreatePolityInput input =
        new CreatePolityInput("Private Room", PolityVisibility.PRIVATE, null, null);

    assertThatThrownBy(() -> service.create(founder, input))
        .isInstanceOf(ApiException.class)
        .hasMessage("Private polities are not available yet.");

    verify(identityUsers, never()).get(any());
    verify(polities, never()).saveAndFlush(any());
    verify(grants, never()).stage(any());
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

    var result = service.getActionAvailability(polityId, userId);

    assertThat(result.inviteMembers().available()).isTrue();
    assertThat(result.introduceMotion().available()).isFalse();
    assertThat(result.introduceMotion().reason()).isEqualTo("polity_provisional");
    assertThat(result.introduceSanction().available()).isFalse();
    assertThat(result.introduceSanction().reason()).isEqualTo("constitutional_office_vacant");
    assertThat(result.introduceDisbandment().available()).isTrue();
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

    assertThat(result.introduceAppeal().available()).isFalse();
    assertThat(result.introduceAppeal().reason()).isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceOfficeTermReview().available()).isFalse();
    assertThat(result.introduceOfficeTermReview().reason())
        .isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceConstitutionalReview().available()).isFalse();
    assertThat(result.introduceConstitutionalReview().reason())
        .isEqualTo("procedure_electorate_office_vacant");
    assertThat(result.introduceDisbandment().available()).isFalse();
    assertThat(result.introduceDisbandment().reason())
        .isEqualTo("procedure_electorate_office_vacant");
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
  void provisionalAdmissionAuthorityRequiresActiveBootstrapStewardTerm() {
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

    assertThat(service.hasProvisionalFounderAdmissionAuthority(founder)).isFalse();
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

  private void stubJudicialProcedure(ConstitutionVersion constitution, String procedureCode) {
    Procedure procedure =
        new Procedure(
            constitution.getPolityId(),
            constitution.getId(),
            UUID.randomUUID(),
            procedureCode,
            procedureCode,
            (ProcedureTemplateKey) null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            2,
            0,
            24,
            EffectType.GRANT_APPEAL);
    when(procedures.findEntityByConstitutionVersionIdAndCode(constitution.getId(), procedureCode))
        .thenReturn(Optional.of(procedure));
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
