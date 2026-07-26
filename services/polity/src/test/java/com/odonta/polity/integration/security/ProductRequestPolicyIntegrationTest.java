package com.odonta.polity.integration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.odonta.polity.PolityApplication;
import io.github.lutzseverino.cardo.authorization.AuthorizationAdminClient;
import io.github.lutzseverino.cardo.billing.client.BillingEntitlementsClient;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import io.github.lutzseverino.cardo.invite.client.InvitationsClient;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Proves the request policy Cardo's product-authentication boundary applies. Reaching a controller
 * is not asserted here; only whether the boundary admitted or refused the request.
 */
@SpringBootTest(
    classes = PolityApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "spring.flyway.baseline-on-migrate=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.modulith.events.jdbc.schema=polity_events",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/realms/test/protocol/openid-connect/certs",
      "cardo.identity.product-auth.identity-session-audience=cardo-identity-test",
      "cardo.identity.product-auth.product-audience=polity-test"
    })
@Import(ProductRequestPolicyIntegrationTest.RestClientConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ProductRequestPolicyIntegrationTest {
  private static final String BASE_PATH = "/api/v1";
  private static final UUID POLITY_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy securityFilterChain;

  @MockitoBean private AuthorizationAdminClient authorization;
  @MockitoBean private BillingEntitlementsClient entitlements;
  @MockitoBean private IdentityUsersClient identityUsers;

  // Admitted public routes reach a controller. Stubbing the outbound clients keeps this test about
  // the boundary decision rather than downstream availability.
  @MockitoBean private InvitationsClient cardoInvitations;

  private MockMvc mockMvc;

  @BeforeEach
  void buildMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
  }

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    properties.add("spring.datasource.username", POSTGRES::getUsername);
    properties.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  void admitsContainerHealthProbes() throws Exception {
    assertAdmitted(get("/actuator/health/readiness"));
    assertAdmitted(get("/actuator/health/liveness"));
  }

  @Test
  void admitsInvitationTokenOnboardingWithoutASession() throws Exception {
    assertAdmitted(get(BASE_PATH + "/invitation-tokens/{token}", "opaque-token"));
    assertAdmitted(get(BASE_PATH + "/invitation-tokens/{token}/completion", "opaque-token"));
    assertAdmitted(post(BASE_PATH + "/invitation-tokens/{token}/completion", "opaque-token"));
  }

  @Test
  void refusesConvergenceSurfacesWithoutAuthentication() throws Exception {
    assertRefused(get(BASE_PATH + "/polity/account"));
    assertRefused(post(BASE_PATH + "/polity/account"));
    assertRefused(get(BASE_PATH + "/polities/{polityId}/members/me/access", POLITY_ID));
    assertRefused(post(BASE_PATH + "/polities/{polityId}/members/me/access", POLITY_ID));
  }

  @Test
  void refusesProductReadsWithoutAuthentication() throws Exception {
    assertRefused(get(BASE_PATH + "/polities"));
    assertRefused(get(BASE_PATH + "/polities/{polityId}", POLITY_ID));
  }

  @Test
  void refusesMethodsTheProductApiDoesNotSupport() throws Exception {
    assertRefused(delete(BASE_PATH + "/polities/{polityId}", POLITY_ID));
  }

  @Test
  void refusesRoutesOutsideTheProductApi() throws Exception {
    assertRefused(get("/internal/metrics"));
    assertRefused(get("/actuator/env"));
  }

  private void assertAdmitted(RequestBuilder request) throws Exception {
    MvcResult result = mockMvc.perform(request).andReturn();
    HttpStatus status = HttpStatus.valueOf(result.getResponse().getStatus());
    assertThat(status)
        .as("boundary admits %s", describe(result))
        .isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  private void assertRefused(RequestBuilder request) throws Exception {
    MvcResult result = mockMvc.perform(request).andReturn();
    HttpStatus status = HttpStatus.valueOf(result.getResponse().getStatus());
    assertThat(status)
        .as("boundary refuses %s", describe(result))
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  private String describe(MvcResult result) {
    return result.getRequest().getMethod() + " " + result.getRequest().getRequestURI();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class RestClientConfiguration {
    @Bean
    RestClient.Builder restClientBuilder() {
      return RestClient.builder();
    }
  }
}
