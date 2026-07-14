package com.odonta.polity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.model.VotingThreshold;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest(
    properties = {
      "spring.flyway.baseline-on-migrate=false",
      "spring.flyway.locations=classpath:db/migration",
      "spring.flyway.table=flyway_schema_history_polity",
      "spring.jpa.hibernate.ddl-auto=validate"
    },
    showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class PolityRepositoryIntegrationTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-12T12:00:00Z");

  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  private final UUID polityId = UUID.randomUUID();
  private final UUID constitutionId = UUID.randomUUID();
  private final UUID jurisdictionId = UUID.randomUUID();
  private final UUID institutionId = UUID.randomUUID();
  private final UUID procedureId = UUID.randomUUID();
  private final UUID membershipId = UUID.randomUUID();
  private final UUID officeId = UUID.randomUUID();

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ConstitutionalPowerRepository constitutionalPowers;
  @Autowired private InstitutionRepository institutions;
  @Autowired private JurisdictionRepository jurisdictions;
  @Autowired private OfficeTermRepository officeTerms;
  @Autowired private PolityRepository polities;
  @Autowired private ProcedureRepository procedures;
  @Autowired private SanctionRepository sanctions;

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    properties.add("spring.datasource.username", POSTGRES::getUsername);
    properties.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void seedGovernment() {
    insertPolity(polityId, UUID.randomUUID(), "Repository Test", "PRIVATE", NOW.minusDays(10));
    jdbc.update(
        """
        insert into public.constitution_versions
          (id, polity_id, version, title, body, status, ratified_at)
        values (?, ?, 1, 'Constitution', 'Durable rules', 'RATIFIED', ?)
        """,
        constitutionId,
        polityId,
        NOW.minusDays(9));
    jdbc.update(
        """
        insert into public.jurisdictions (id, polity_id, name, kind)
        values (?, ?, 'Root jurisdiction', 'ROOT')
        """,
        jurisdictionId,
        polityId);
    jdbc.update(
        """
        insert into public.institutions
          (id, polity_id, jurisdiction_id, constitution_version_id, name, name_key, kind)
        values (?, ?, ?, ?, 'Council', 'institution.council.name', 'COUNCIL')
        """,
        institutionId,
        polityId,
        jurisdictionId,
        constitutionId);
    jdbc.update(
        """
        insert into public.procedures
          (id, polity_id, constitution_version_id, institution_id, code, name, name_key,
           quorum_numerator, quorum_denominator, threshold, effect_type,
           minimum_notice_hours, voting_period_hours, electorate, minimum_elector_count)
        values (?, ?, ?, ?, 'ordinary-resolution', 'Ordinary resolution',
                'procedure.ordinary-resolution.name', 1, 2, 'SIMPLE_MAJORITY_CAST',
                'ADOPT_RESOLUTION', 0, 24, 'ACTIVE_MEMBERS', 1)
        """,
        procedureId,
        polityId,
        constitutionId,
        institutionId);
    insertMembership(polityId, membershipId, UUID.randomUUID(), "Active member", "ACTIVE", null);
    jdbc.update(
        """
        insert into public.offices
          (id, polity_id, constitution_version_id, jurisdiction_id, code, name, description,
           term_length_days, seat_count, name_key, description_key)
        values (?, ?, ?, ?, 'steward', 'Steward', 'Executes adopted motions', 30, 1,
                'office.steward.name', 'office.steward.description')
        """,
        officeId,
        polityId,
        constitutionId,
        jurisdictionId);
    jdbc.update(
        """
        insert into public.constitutional_powers
          (id, polity_id, constitution_version_id, code, name, name_key, holder_scope)
        values (?, ?, ?, 'INTRODUCE_MOTION', 'Introduce motion',
                'power.introduce-motion.name', 'ACTIVE_MEMBER')
        """,
        UUID.randomUUID(),
        polityId,
        constitutionId);
  }

  @Test
  void renamedEntityProjectionsMaterializeTheirOwnedFields() {
    JurisdictionProjection jurisdiction =
        jurisdictions.findProjectionsByPolityId(polityId).getFirst();
    InstitutionProjection institution =
        institutions.findProjectionsByConstitutionVersionId(constitutionId).getFirst();
    ProcedureProjection procedure =
        procedures.findProjectionsByConstitutionVersionId(constitutionId).getFirst();
    ConstitutionalPowerProjection power =
        constitutionalPowers.findProjectionsByConstitutionVersionId(constitutionId).getFirst();

    assertThat(jurisdiction.getId()).isEqualTo(jurisdictionId);
    assertThat(jurisdiction.getPolityId()).isEqualTo(polityId);
    assertThat(jurisdiction.getKind()).isEqualTo(JurisdictionKind.ROOT);
    assertThat(institution.getId()).isEqualTo(institutionId);
    assertThat(institution.getConstitutionVersionId()).isEqualTo(constitutionId);
    assertThat(institution.getNameKey()).isEqualTo("institution.council.name");
    assertThat(institution.getKind()).isEqualTo(InstitutionKind.COUNCIL);
    assertThat(procedure.getId()).isEqualTo(procedureId);
    assertThat(procedure.getElectorate()).isEqualTo(ProcedureElectorate.ACTIVE_MEMBERS);
    assertThat(procedure.getThreshold()).isEqualTo(VotingThreshold.SIMPLE_MAJORITY_CAST);
    assertThat(procedure.getEffectType()).isEqualTo(EffectType.ADOPT_RESOLUTION);
    assertThat(power.getConstitutionVersionId()).isEqualTo(constitutionId);
    assertThat(power.getCode()).isEqualTo(PowerCode.INTRODUCE_MOTION);
    assertThat(power.getHolderScope()).isEqualTo(PowerHolderScope.ACTIVE_MEMBER);
    assertThat(power.getNameKey()).isEqualTo("power.introduce-motion.name");
  }

  @Test
  void accessiblePolitiesRespectVisibilityActiveMembershipAndTenantOwnership() {
    UUID requestingUserId = UUID.randomUUID();
    UUID publicPolityId = UUID.randomUUID();
    UUID memberPolityId = UUID.randomUUID();
    UUID resignedPolityId = UUID.randomUUID();
    UUID otherUsersPolityId = UUID.randomUUID();
    insertPolity(publicPolityId, UUID.randomUUID(), "Public", "PUBLIC", NOW.minusDays(4));
    insertPolity(memberPolityId, UUID.randomUUID(), "Member private", "PRIVATE", NOW.minusDays(3));
    insertPolity(
        resignedPolityId, UUID.randomUUID(), "Former private", "PRIVATE", NOW.minusDays(2));
    insertPolity(
        otherUsersPolityId, UUID.randomUUID(), "Other private", "PRIVATE", NOW.minusDays(1));
    insertMembership(
        memberPolityId, UUID.randomUUID(), requestingUserId, "Current member", "ACTIVE", null);
    insertMembership(
        resignedPolityId,
        UUID.randomUUID(),
        requestingUserId,
        "Former member",
        "RESIGNED",
        NOW.minusHours(1));
    insertMembership(
        otherUsersPolityId, UUID.randomUUID(), UUID.randomUUID(), "Different user", "ACTIVE", null);

    List<PolityProjection> accessible =
        polities
            .findAccessibleProjections(
                requestingUserId,
                MembershipStatus.ACTIVE,
                PolityVisibility.PUBLIC,
                PageRequest.of(0, 50))
            .getContent();

    assertThat(accessible)
        .extracting(PolityProjection::getId)
        .containsExactly(memberPolityId, publicPolityId);
  }

  @Test
  void collectionQueriesHandleEmptyInputsAndRestrictNonEmptyInputs() {
    UUID motionId = insertMotion(EffectType.APPLY_SANCTION);
    insertSanction(motionId, membershipId, NOW.plusDays(1));
    insertOfficeTerm(membershipId, "steward", NOW.plusDays(1));

    assertThat(jurisdictions.findProjectionsByPolityIdInAndKind(List.of(), JurisdictionKind.ROOT))
        .isEmpty();
    assertThat(
            sanctions
                .findProjectionsByPolityIdAndTargetMembershipIdInAndTypeAndStatusAndEndsAtAfter(
                    polityId, List.of(), SanctionType.SUSPENSION, SanctionStatus.ACTIVE, NOW))
        .isEmpty();
    assertThat(
            officeTerms.findProjectionsByPolityIdAndOfficeCodeInAndStatusAndEndsAtAfter(
                polityId, List.of(), OfficeTermStatus.ACTIVE, NOW))
        .isEmpty();
    assertThat(officeTerms.findProjectionsByPolityIdAndIdIn(polityId, List.of())).isEmpty();

    assertThat(
            jurisdictions.findProjectionsByPolityIdInAndKind(
                List.of(polityId), JurisdictionKind.ROOT))
        .extracting(JurisdictionProjection::getId)
        .containsExactly(jurisdictionId);
    assertThat(
            sanctions
                .findProjectionsByPolityIdAndTargetMembershipIdInAndTypeAndStatusAndEndsAtAfter(
                    polityId,
                    List.of(membershipId),
                    SanctionType.SUSPENSION,
                    SanctionStatus.ACTIVE,
                    NOW))
        .hasSize(1);
    assertThat(
            officeTerms.findProjectionsByPolityIdAndOfficeCodeInAndStatusAndEndsAtAfter(
                polityId, List.of("steward"), OfficeTermStatus.ACTIVE, NOW))
        .hasSize(1);
  }

  @Test
  void targetedSanctionAndOfficeTermQueriesUseStrictEndsAtBoundary() {
    UUID elapsedMotionId = insertMotion(EffectType.APPLY_SANCTION);
    UUID boundaryMotionId = insertMotion(EffectType.APPLY_SANCTION);
    UUID futureMotionId = insertMotion(EffectType.APPLY_SANCTION);
    insertSanction(elapsedMotionId, membershipId, NOW.minusSeconds(1));
    insertSanction(boundaryMotionId, membershipId, NOW);
    UUID futureSanctionId = insertSanction(futureMotionId, membershipId, NOW.plusSeconds(1));
    insertOfficeTerm(membershipId, "steward", NOW.minusSeconds(1));
    insertOfficeTerm(membershipId, "steward", NOW);
    UUID futureOfficeTermId = insertOfficeTerm(membershipId, "steward", NOW.plusSeconds(1));

    assertThat(
            sanctions
                .findProjectionsByPolityIdAndTargetMembershipIdInAndTypeAndStatusAndEndsAtAfter(
                    polityId,
                    List.of(membershipId),
                    SanctionType.SUSPENSION,
                    SanctionStatus.ACTIVE,
                    NOW))
        .extracting(SanctionProjection::getId)
        .containsExactly(futureSanctionId);
    assertThat(
            officeTerms.findProjectionsByPolityIdAndMembershipIdAndStatusAndEndsAtAfter(
                polityId, membershipId, OfficeTermStatus.ACTIVE, NOW))
        .extracting(OfficeTermProjection::getId)
        .containsExactly(futureOfficeTermId);
    assertThat(
            officeTerms.findProjectionsByPolityIdAndOfficeCodeInAndStatusAndEndsAtAfter(
                polityId, List.of("steward"), OfficeTermStatus.ACTIVE, NOW))
        .extracting(OfficeTermProjection::getId)
        .containsExactly(futureOfficeTermId);
  }

  @Test
  void flywayAppliesBaselineAndActiveGovernmentIndexes() {
    List<String> successfulVersions =
        jdbc.queryForList(
            """
            select version
            from public.flyway_schema_history_polity
            where success
            order by installed_rank
            """,
            String.class);
    Map<String, String> indexes =
        jdbc
            .query(
                """
                select indexname, indexdef
                from pg_indexes
                where schemaname = 'public'
                  and indexname in (
                    'idx_sanctions_active_target',
                    'idx_office_terms_active_membership',
                    'idx_office_terms_active_office')
                """,
                (result, rowNumber) -> Map.entry(result.getString(1), result.getString(2)))
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    assertThat(successfulVersions).containsExactly("1", "2");
    assertThat(indexes)
        .containsOnlyKeys(
            "idx_sanctions_active_target",
            "idx_office_terms_active_membership",
            "idx_office_terms_active_office");
    assertThat(indexes.values()).allMatch(definition -> definition.contains("status = 'ACTIVE'"));
  }

  private void insertPolity(
      UUID id, UUID founderId, String name, String visibility, OffsetDateTime createdAt) {
    jdbc.update(
        """
        insert into public.polities
          (id, founder_id, name, visibility, status, created_at, updated_at)
        values (?, ?, ?, ?, 'ACTIVE', ?, ?)
        """,
        id,
        founderId,
        name,
        visibility,
        createdAt,
        createdAt);
  }

  private void insertMembership(
      UUID memberPolityId,
      UUID memberId,
      UUID userId,
      String displayName,
      String status,
      OffsetDateTime resignedAt) {
    jdbc.update(
        """
        insert into public.memberships
          (id, polity_id, user_id, authorization_subject, email, display_name,
           status, admitted_at, resigned_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        memberId,
        memberPolityId,
        userId,
        "subject-" + memberId,
        memberId + "@example.test",
        displayName,
        status,
        NOW.minusDays(5),
        resignedAt);
  }

  private UUID insertMotion(EffectType effectType) {
    UUID motionId = UUID.randomUUID();
    jdbc.update(
        """
        insert into public.motions
          (id, polity_id, jurisdiction_id, institution_id, constitution_version_id,
           procedure_id, introduced_by, title, body, status, effect_type, opened_at,
           voting_opens_at, voting_closes_at, certification_opens_at)
        values (?, ?, ?, ?, ?, ?, ?, 'Test motion', 'Repository fixture', 'ENACTED', ?, ?, ?, ?, ?)
        """,
        motionId,
        polityId,
        jurisdictionId,
        institutionId,
        constitutionId,
        procedureId,
        membershipId,
        effectType.name(),
        NOW.minusDays(3),
        NOW.minusDays(3),
        NOW.minusDays(2),
        NOW.minusDays(2));
    return motionId;
  }

  private UUID insertSanction(UUID motionId, UUID targetMembershipId, OffsetDateTime endsAt) {
    UUID sanctionId = UUID.randomUUID();
    jdbc.update(
        """
        insert into public.sanctions
          (id, polity_id, motion_id, target_membership_id, type, status, reason,
           started_at, ends_at)
        values (?, ?, ?, ?, 'SUSPENSION', 'ACTIVE', 'Repository fixture', ?, ?)
        """,
        sanctionId,
        polityId,
        motionId,
        targetMembershipId,
        endsAt.minusDays(1),
        endsAt);
    return sanctionId;
  }

  private UUID insertOfficeTerm(UUID holderMembershipId, String officeCode, OffsetDateTime endsAt) {
    UUID officeTermId = UUID.randomUUID();
    jdbc.update(
        """
        insert into public.office_terms
          (id, polity_id, office_id, office_code, membership_id, status, started_at, ends_at)
        values (?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
        """,
        officeTermId,
        polityId,
        officeId,
        officeCode,
        holderMembershipId,
        endsAt.minusDays(1),
        endsAt);
    return officeTermId;
  }
}
