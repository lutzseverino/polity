package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.input.CreatePolityInput;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationAcceptanceStatus;
import com.odonta.polity.model.MembershipInvitationStatus;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.resolver.PolitySummaryResolver;
import com.odonta.polity.service.MembershipAccessService;
import com.odonta.polity.service.OfficialRecordService;
import com.odonta.polity.service.PolitySlugService;
import com.odonta.polity.template.ConstitutionTemplateSeeder;
import io.github.lutzseverino.cardo.authorization.AuthorizationAdminClient;
import io.github.lutzseverino.cardo.authorization.grant.AuthorizationPlanConfiguration;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.billing.client.BillingEntitlementsClient;
import io.github.lutzseverino.cardo.identity.client.IdentityUser;
import io.github.lutzseverino.cardo.identity.client.IdentityUserStatus;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest(
    properties = {
      "spring.flyway.baseline-on-migrate=false",
      "spring.flyway.locations=classpath:db/migration,classpath:db/authorization/publications",
      "spring.flyway.placeholders.authorizationSchema=polity_events",
      "spring.flyway.table=flyway_schema_history_membership_access_grants",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.modulith.events.jdbc.schema=polity_events",
      "cardo.authorization.plans.max-attempts=1"
    },
    showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
  AuthorizationPlanConfiguration.class,
  CreatePolityWorkflow.class,
  CompleteMembershipInvitationWorkflow.class,
  MembershipAccessService.class,
  PolityGrantPlanner.class,
  MembershipAccessGrantIntegrationTest.FixedClockConfiguration.class
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MembershipAccessGrantIntegrationTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-24T12:00:00Z");

  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Autowired private CompleteMembershipInvitationWorkflow completion;
  @Autowired private ConstitutionVersionRepository constitutions;
  @Autowired private CreatePolityWorkflow creation;
  @Autowired private Grants grants;
  @Autowired private JurisdictionRepository jurisdictions;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MembershipAccessService access;
  @Autowired private MembershipInvitationRepository invitations;
  @Autowired private MembershipRepository memberships;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private PolityRepository polities;

  @MockitoBean private AuthorizationAdminClient authorization;
  @MockitoBean private BillingEntitlementsClient entitlements;
  @MockitoBean private PolityBootstrapCompleter bootstrap;
  @MockitoBean private ConstitutionTemplateSeeder templates;
  @MockitoBean private IdentityUsersClient identityUsers;
  @MockitoBean private IncompleteEventPublications incompletePublications;
  @MockitoBean private OfficeRepository offices;
  @MockitoBean private OfficeTermRepository officeTerms;
  @MockitoBean private OfficialRecordService records;
  @MockitoBean private PolityContextResolver polityContext;
  @MockitoBean private PolitySlugService slugs;
  @MockitoBean private PolitySummaryResolver summaries;

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    properties.add("spring.datasource.username", POSTGRES::getUsername);
    properties.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  void invitedCompletionCommitsMembershipReceiptInvitationAndGrantPublicationTogether() {
    CompletionFixture fixture = seedRequestedInvitation(null);
    long receiptsBefore = receiptCount();
    long publicationsBefore = publicationCount();

    completion.complete(fixture.invitationId(), fixture.identity(), NOW);

    MembershipInvitation invitation = invitations.findById(fixture.invitationId()).orElseThrow();
    Membership membership =
        memberships
            .findEntityByPolityIdAndUserId(fixture.polityId(), fixture.identity().id())
            .orElseThrow();
    assertThat(invitation.getStatus()).isEqualTo(MembershipInvitationStatus.ACCEPTED);
    assertThat(invitation.getAcceptanceStatus())
        .isEqualTo(MembershipInvitationAcceptanceStatus.COMPLETED);
    assertThat(membership.getGrantReceiptId()).isNotNull();
    assertThat(grants.find(membership.getGrantReceiptId())).isPresent();
    assertThat(receiptCount()).isEqualTo(receiptsBefore + 1);
    assertThat(publicationCount()).isEqualTo(publicationsBefore + 1);
  }

  @Test
  void failureAfterStagingRollsBackMembershipReceiptInvitationAndGrantPublicationTogether() {
    CompletionFixture fixture = seedRequestedInvitation(null);
    long receiptsBefore = receiptCount();
    long publicationsBefore = publicationCount();
    doThrow(new IllegalStateException("official record unavailable"))
        .when(records)
        .append(any(), any(), any(), any(), any(), any(), any(), any(), any());

    assertThatThrownBy(() -> completion.complete(fixture.invitationId(), fixture.identity(), NOW))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("official record unavailable");

    MembershipInvitation invitation = invitations.findById(fixture.invitationId()).orElseThrow();
    assertThat(invitation.getStatus()).isEqualTo(MembershipInvitationStatus.PENDING);
    assertThat(invitation.getAcceptanceStatus())
        .isEqualTo(MembershipInvitationAcceptanceStatus.REQUESTED);
    assertThat(
            memberships.findEntityByPolityIdAndUserId(fixture.polityId(), fixture.identity().id()))
        .isEmpty();
    assertThat(receiptCount()).isEqualTo(receiptsBefore);
    assertThat(publicationCount()).isEqualTo(publicationsBefore);
  }

  @Test
  void concurrentCompletionRetriesStageExactlyOneReceiptAndPublication() throws Exception {
    CompletionFixture fixture = seedRequestedInvitation(null);
    long receiptsBefore = receiptCount();
    long publicationsBefore = publicationCount();
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try (var executor = Executors.newFixedThreadPool(2)) {
      Future<?> first = submitCompletion(executor, ready, start, fixture);
      Future<?> second = submitCompletion(executor, ready, start, fixture);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
    }

    Membership membership =
        memberships
            .findEntityByPolityIdAndUserId(fixture.polityId(), fixture.identity().id())
            .orElseThrow();
    assertThat(membership.getGrantReceiptId()).isNotNull();
    assertThat(receiptCount()).isEqualTo(receiptsBefore + 1);
    assertThat(publicationCount()).isEqualTo(publicationsBefore + 1);
  }

  @Test
  void reactivationReplacesTheFormerReceiptAndCommitsOneNewGrantPublication() {
    Membership resigned = seedResignedMembership();
    UUID formerReceiptId = resigned.getGrantReceiptId();
    CompletionFixture fixture = seedRequestedInvitation(resigned);
    long receiptsBefore = receiptCount();
    long publicationsBefore = publicationCount();

    completion.complete(fixture.invitationId(), fixture.identity(), NOW);

    Membership reactivated = memberships.findById(resigned.getId()).orElseThrow();
    assertThat(reactivated.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    assertThat(reactivated.getGrantReceiptId()).isNotEqualTo(formerReceiptId);
    assertThat(grants.find(reactivated.getGrantReceiptId())).isPresent();
    assertThat(receiptCount()).isEqualTo(receiptsBefore + 1);
    assertThat(publicationCount()).isEqualTo(publicationsBefore + 1);
  }

  @Test
  void legacyReconciliationPersistsAndReusesOneReceipt() {
    Membership legacy = seedActiveMembershipWithoutReceipt();
    long receiptsBefore = receiptCount();
    long publicationsBefore = publicationCount();

    var first = access.reconcile(legacy.getPolityId(), legacy.getUserId());
    var repeated = access.reconcile(legacy.getPolityId(), legacy.getUserId());

    Membership converged = memberships.findById(legacy.getId()).orElseThrow();
    assertThat(first.receiptId()).isEqualTo(converged.getGrantReceiptId());
    assertThat(repeated.receiptId()).isEqualTo(first.receiptId());
    assertThat(receiptCount()).isEqualTo(receiptsBefore + 1);
    assertThat(publicationCount()).isEqualTo(publicationsBefore + 1);
  }

  @Test
  void founderCreationRetainsItsReceiptOnThePersistedMembership() {
    UUID founderId = UUID.randomUUID();
    AuthenticatedUser founder =
        new AuthenticatedUser(founderId, "subject-founder-" + founderId, "Founder");
    IdentityUser identity = identity(founderId, "founder-" + founderId + "@example.com");
    String slug = "founder-" + founderId;
    when(identityUsers.get(founderId)).thenReturn(identity);
    when(slugs.claim(any())).thenReturn(slug);
    when(offices.findEntityByConstitutionVersionIdAndCode(any(), eq(Office.STEWARD)))
        .thenAnswer(
            invocation -> {
              Office office =
                  new Office(
                      UUID.randomUUID(),
                      invocation.getArgument(0),
                      UUID.randomUUID(),
                      Office.STEWARD,
                      "Steward",
                      "Steward",
                      30);
              ReflectionTestUtils.setField(office, "id", UUID.randomUUID());
              return Optional.of(office);
            });
    when(officeTerms.saveAndFlush(any(OfficeTerm.class)))
        .thenAnswer(
            invocation -> {
              OfficeTerm term = invocation.getArgument(0);
              ReflectionTestUtils.setField(term, "id", UUID.randomUUID());
              return term;
            });
    long receiptsBefore = receiptCount();
    long publicationsBefore = publicationCount();

    creation.create(
        founder, new CreatePolityInput("Founder receipt", PolityVisibility.PUBLIC, null, null));

    Polity polity =
        polities
            .findProjectedBySlug(slug)
            .map(result -> polities.getReferenceById(result.getId()))
            .orElseThrow();
    Membership membership =
        memberships.findEntityByPolityIdAndUserId(polity.getId(), founderId).orElseThrow();
    assertThat(membership.getGrantReceiptId()).isNotNull();
    assertThat(grants.find(membership.getGrantReceiptId())).isPresent();
    assertThat(receiptCount()).isEqualTo(receiptsBefore + 1);
    assertThat(publicationCount()).isEqualTo(publicationsBefore + 1);
  }

  private Future<?> submitCompletion(
      java.util.concurrent.ExecutorService executor,
      CountDownLatch ready,
      CountDownLatch start,
      CompletionFixture fixture) {
    return executor.submit(
        () -> {
          ready.countDown();
          assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
          completion.complete(fixture.invitationId(), fixture.identity(), NOW);
          return null;
        });
  }

  private CompletionFixture seedRequestedInvitation(Membership existing) {
    return transaction(
        () -> {
          UUID userId = existing == null ? UUID.randomUUID() : existing.getUserId();
          Polity polity =
              existing == null
                  ? polities.saveAndFlush(
                      new Polity(
                          "Invitation " + UUID.randomUUID(),
                          "invitation-" + UUID.randomUUID(),
                          PolityVisibility.PRIVATE,
                          UUID.randomUUID()))
                  : polities.findById(existing.getPolityId()).orElseThrow();
          Membership inviter = membership(polity.getId(), UUID.randomUUID(), "inviter");
          memberships.saveAndFlush(inviter);
          MembershipInvitation invitation =
              new MembershipInvitation(
                  polity.getId(),
                  "member-" + userId + "@example.com",
                  inviter.getId(),
                  NOW.minusDays(1));
          invitation.registerCardoInvitation(UUID.randomUUID(), userId, NOW.plusDays(1));
          invitation.requestAcceptance(NOW.minusMinutes(1));
          invitations.saveAndFlush(invitation);
          configureAdmissionContext(polity.getId());
          return new CompletionFixture(
              polity.getId(), invitation.getId(), identity(userId, invitation.getEmail()));
        });
  }

  private Membership seedResignedMembership() {
    return transaction(
        () -> {
          Polity polity =
              polities.saveAndFlush(
                  new Polity(
                      "Reactivation " + UUID.randomUUID(),
                      "reactivation-" + UUID.randomUUID(),
                      PolityVisibility.PRIVATE,
                      UUID.randomUUID()));
          Membership membership = membership(polity.getId(), UUID.randomUUID(), "former");
          membership.retainGrantReceipt(UUID.randomUUID());
          membership.resign(NOW.minusDays(1));
          return memberships.saveAndFlush(membership);
        });
  }

  private Membership seedActiveMembershipWithoutReceipt() {
    return transaction(
        () -> {
          Polity polity =
              polities.saveAndFlush(
                  new Polity(
                      "Legacy " + UUID.randomUUID(),
                      "legacy-" + UUID.randomUUID(),
                      PolityVisibility.PRIVATE,
                      UUID.randomUUID()));
          return memberships.saveAndFlush(membership(polity.getId(), UUID.randomUUID(), "legacy"));
        });
  }

  private Membership membership(UUID polityId, UUID userId, String label) {
    return new Membership(
        polityId,
        userId,
        "subject-" + label + "-" + userId,
        label + "-" + userId + "@example.com",
        label,
        NOW.minusDays(2),
        null);
  }

  private IdentityUser identity(UUID userId, String email) {
    return new IdentityUser(
        userId,
        "subject-member-" + userId,
        email,
        "Member",
        null,
        IdentityUserStatus.ACTIVE,
        true,
        NOW.minusDays(3),
        NOW.minusDays(2));
  }

  private void configureAdmissionContext(UUID polityId) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Constitution", "Body", NOW.minusDays(3));
    ReflectionTestUtils.setField(constitution, "id", UUID.randomUUID());
    Jurisdiction jurisdiction =
        new Jurisdiction(polityId, "Root", com.odonta.polity.model.JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    when(polityContext.constitution(polityId)).thenReturn(constitution);
    when(polityContext.rootJurisdiction(polityId)).thenReturn(jurisdiction);
  }

  private long receiptCount() {
    return jdbc.queryForObject("select count(*) from polity_events.grant_receipt", Long.class);
  }

  private long publicationCount() {
    return jdbc.queryForObject("select count(*) from polity_events.event_publication", Long.class);
  }

  private <T> T transaction(java.util.function.Supplier<T> work) {
    return new TransactionTemplate(transactionManager).execute(status -> work.get());
  }

  private record CompletionFixture(UUID polityId, UUID invitationId, IdentityUser identity) {}

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClockConfiguration {
    @Bean
    Clock clock() {
      return Clock.fixed(NOW.toInstant(), ZoneOffset.UTC);
    }
  }
}
