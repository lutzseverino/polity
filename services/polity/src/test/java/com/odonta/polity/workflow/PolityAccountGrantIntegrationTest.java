package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.repository.PolityAccountRepository;
import com.odonta.polity.result.GrantConvergenceResult;
import com.odonta.polity.service.GrantConvergenceService;
import com.odonta.polity.service.PolityAccountService;
import io.github.lutzseverino.cardo.authorization.AuthorizationAdminClient;
import io.github.lutzseverino.cardo.authorization.grant.AuthorizationPlanConfiguration;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceiptStatus;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
      "spring.flyway.table=flyway_schema_history_polity_account_grants",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.modulith.events.jdbc.schema=polity_events",
      "cardo.authorization.plans.max-attempts=1"
    },
    showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
  AuthorizationPlanConfiguration.class,
  GrantConvergenceService.class,
  PolityAccountService.class,
  PolityGrantPlanner.class,
  ProvisionPolityAccountWorkflow.class
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PolityAccountGrantIntegrationTest {
  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Autowired private PolityAccountRepository accounts;
  @Autowired private GrantConvergenceService convergence;
  @Autowired private Grants grants;
  @Autowired private PolityAccountService polityAccounts;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private ProvisionPolityAccountWorkflow workflow;

  @MockitoBean private AuthorizationAdminClient authorization;
  @MockitoBean private IncompleteEventPublications incompletePublications;

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    properties.add("spring.datasource.username", POSTGRES::getUsername);
    properties.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void failProviderApplication() {
    doThrow(new IllegalStateException("provider unavailable"))
        .when(authorization)
        .ensureClientRolesAssigned(any());
  }

  @Test
  void commitsTheAccountAndReceiptTogetherThenPollsThePersistedTerminalState()
      throws InterruptedException {
    AuthenticatedUser user = user();
    var provisioned =
        new TransactionTemplate(transactionManager).execute(status -> workflow.provision(user));

    assertThat(provisioned).isNotNull();
    assertThat(provisioned.created()).isTrue();
    assertThat(provisioned.account().grants().status()).isEqualTo(GrantReceiptStatus.PENDING);
    UUID receiptId = provisioned.account().grants().receiptId();
    assertThat(accounts.findById(user.id()))
        .get()
        .extracting(account -> account.getGrantReceiptId())
        .isEqualTo(receiptId);

    GrantConvergenceResult terminal = awaitTerminal(receiptId);
    assertThat(terminal.status()).isEqualTo(GrantReceiptStatus.FAILED);
    assertThat(terminal.failureCode()).isEqualTo("provider_application_failed");
    assertThat(polityAccounts.get(user.id()).grants()).isEqualTo(terminal);
  }

  @Test
  void rollsBackTheAccountAndReceiptTogether() {
    AuthenticatedUser user = user();
    AtomicReference<UUID> receiptId = new AtomicReference<>();
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              receiptId.set(workflow.provision(user).account().grants().receiptId());
              status.setRollbackOnly();
            });

    assertThat(accounts.findById(user.id())).isEmpty();
    assertThat(grants.find(receiptId.get())).isEmpty();
  }

  private GrantConvergenceResult awaitTerminal(UUID receiptId) throws InterruptedException {
    Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
    GrantConvergenceResult current = convergence.get(receiptId);
    while (current.status() == GrantReceiptStatus.PENDING && Instant.now().isBefore(deadline)) {
      Thread.sleep(25);
      current = convergence.get(receiptId);
    }
    return current;
  }

  private AuthenticatedUser user() {
    return new AuthenticatedUser(UUID.randomUUID(), "subject-" + UUID.randomUUID(), "Citizen");
  }
}
