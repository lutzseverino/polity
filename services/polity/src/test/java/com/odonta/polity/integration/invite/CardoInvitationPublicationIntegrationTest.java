package com.odonta.polity.integration.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.PolityApplication;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationAcceptanceStatus;
import com.odonta.polity.model.MembershipInvitationStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.resolver.GovernmentAssessmentResolver;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.service.OfficialRecordService;
import com.odonta.polity.service.PolityService;
import com.odonta.polity.workflow.AcceptMembershipInvitationWorkflow;
import io.github.lutzseverino.cardo.authorization.AuthorizationAdminClient;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.billing.client.BillingEntitlementsClient;
import io.github.lutzseverino.cardo.common.api.ApiException;
import io.github.lutzseverino.cardo.identity.client.IdentityUser;
import io.github.lutzseverino.cardo.identity.client.IdentityUserStatus;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import io.github.lutzseverino.cardo.invite.client.Invitation;
import io.github.lutzseverino.cardo.invite.client.InvitationStatus;
import io.github.lutzseverino.cardo.invite.client.InvitationsClient;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    classes = PolityApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "spring.flyway.baseline-on-migrate=true",
      "spring.flyway.locations=classpath:db/migration,classpath:db/authorization/publications",
      "spring.flyway.placeholders.authorizationSchema=polity_events",
      "spring.flyway.table=flyway_schema_history_cardo_invitation_publications",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.modulith.events.jdbc.schema=polity_events",
      "cardo.authorization.plans.max-attempts=1",
      "polity.membership-invitations.retry-delay=PT24H"
    })
@Import(CardoInvitationPublicationIntegrationTest.DeterministicAsyncConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CardoInvitationPublicationIntegrationTest {
  private static final OffsetDateTime ACCEPTED_AT = OffsetDateTime.parse("2026-07-24T12:34:56Z");
  private static final String ACCEPTANCE_LISTENER = "polity.cardo-invitation-acceptance";

  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Autowired private AcceptMembershipInvitationWorkflow acceptance;
  @Autowired private IncompleteEventPublications publications;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MembershipInvitationRepository invitations;
  @Autowired private MembershipRepository memberships;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private PolityRepository polities;

  @MockitoBean private AuthorizationAdminClient authorization;
  @MockitoBean private BillingEntitlementsClient entitlements;
  @MockitoBean private Clock clock;
  @MockitoBean private GovernmentAssessmentResolver governmentAssessments;
  @MockitoBean private IdentityUsersClient identityUsers;
  @MockitoBean private InvitationsClient inviteClient;
  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private OfficialRecordService records;
  @MockitoBean private PolityContextResolver polityContext;
  @MockitoBean private PolityService polityService;

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    properties.add("spring.datasource.username", POSTGRES::getUsername);
    properties.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void useOriginalAcceptanceTime() {
    when(clock.instant()).thenReturn(ACCEPTED_AT.toInstant());
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
  }

  @Test
  void retryablePublicationSurvivesResubmissionAndCompletesAdmissionOnceWithOriginalTimestamp() {
    Fixture fixture = seedInvitation();
    when(identityUsers.get(fixture.identity().id())).thenReturn(fixture.identity());
    when(inviteClient.accept(eq(fixture.cardoInvitationId()), any(OffsetDateTime.class)))
        .thenThrow(ApiException.of(503, "invite_unavailable", "Invite unavailable."))
        .thenThrow(ApiException.of(503, "invite_unavailable", "Invite unavailable."))
        .thenReturn(acceptedInvitation(fixture));

    var intent = acceptance.accept(fixture.invitationId(), fixture.actor());

    assertThat(intent.status()).isEqualTo(MembershipInvitationAcceptanceStatus.REQUESTED);
    assertThat(intent.requestedAt()).isEqualTo(ACCEPTED_AT);
    MembershipInvitation requested = invitations.findById(fixture.invitationId()).orElseThrow();
    assertThat(requested.getAcceptanceStatus())
        .isEqualTo(MembershipInvitationAcceptanceStatus.REQUESTED);
    assertThat(requested.getAcceptanceRequestedAt()).isEqualTo(ACCEPTED_AT);
    Publication firstFailure = acceptancePublication(fixture.invitationId());
    assertThat(firstFailure.serializedEvent()).contains(ACCEPTED_AT.toString());
    assertThat(firstFailure.completionDate()).isNull();

    retryIncomplete();

    Publication failedResubmission = acceptancePublication(fixture.invitationId());
    assertThat(failedResubmission.completionDate()).isNull();
    assertThat(failedResubmission.lastResubmissionDate()).isNotNull();
    verify(inviteClient, times(2)).accept(fixture.cardoInvitationId(), ACCEPTED_AT);

    retryIncomplete();

    Publication completed = acceptancePublication(fixture.invitationId());
    assertThat(completed.completionDate()).isNotNull();
    MembershipInvitation invitation = invitations.findById(fixture.invitationId()).orElseThrow();
    Membership membership =
        memberships
            .findEntityByPolityIdAndUserId(fixture.polityId(), fixture.identity().id())
            .orElseThrow();
    assertThat(invitation.getStatus()).isEqualTo(MembershipInvitationStatus.ACCEPTED);
    assertThat(invitation.getAcceptanceStatus())
        .isEqualTo(MembershipInvitationAcceptanceStatus.COMPLETED);
    assertThat(invitation.getRespondedAt()).isEqualTo(ACCEPTED_AT);
    assertThat(membership.getGrantReceiptId()).isNotNull();
    assertThat(membershipCount(fixture)).isOne();
    verify(records, times(1)).append(any(), any(), any(), any(), any(), any(), any(), any(), any());

    retryIncomplete();

    ArgumentCaptor<OffsetDateTime> timestamps = ArgumentCaptor.forClass(OffsetDateTime.class);
    verify(inviteClient, times(3)).accept(eq(fixture.cardoInvitationId()), timestamps.capture());
    assertThat(timestamps.getAllValues()).containsExactly(ACCEPTED_AT, ACCEPTED_AT, ACCEPTED_AT);
    assertThat(membershipCount(fixture)).isOne();
  }

  @Test
  void terminalPublicationPersistsFailureAndIsCompletedWithoutRecoveryRetry() {
    Fixture fixture = seedInvitation();
    when(identityUsers.get(fixture.identity().id())).thenReturn(fixture.identity());
    when(inviteClient.accept(fixture.cardoInvitationId(), ACCEPTED_AT))
        .thenThrow(ApiException.gone("invitation_expired", "Invitation expired."));

    acceptance.accept(fixture.invitationId(), fixture.actor());

    MembershipInvitation failed = invitations.findById(fixture.invitationId()).orElseThrow();
    assertThat(failed.getStatus()).isEqualTo(MembershipInvitationStatus.CANCELLED);
    assertThat(failed.getAcceptanceStatus()).isEqualTo(MembershipInvitationAcceptanceStatus.FAILED);
    assertThat(failed.getAcceptanceRequestedAt()).isEqualTo(ACCEPTED_AT);
    assertThat(failed.getAcceptanceFailureCode()).isEqualTo("invitation_expired");
    assertThat(acceptancePublication(fixture.invitationId()).completionDate()).isNotNull();
    assertThat(membershipCount(fixture)).isZero();

    retryIncomplete();

    verify(inviteClient, times(1)).accept(fixture.cardoInvitationId(), ACCEPTED_AT);
    assertThat(acceptancePublication(fixture.invitationId()).completionDate()).isNotNull();
    assertThat(membershipCount(fixture)).isZero();
  }

  private Fixture seedInvitation() {
    return new TransactionTemplate(transactionManager)
        .execute(
            status -> {
              UUID userId = UUID.randomUUID();
              Polity polity =
                  polities.saveAndFlush(
                      new Polity(
                          "Publication " + UUID.randomUUID(),
                          "publication-" + UUID.randomUUID(),
                          PolityVisibility.PRIVATE,
                          UUID.randomUUID()));
              Membership inviter =
                  memberships.saveAndFlush(
                      membership(polity.getId(), UUID.randomUUID(), "inviter"));
              MembershipInvitation invitation =
                  new MembershipInvitation(
                      polity.getId(),
                      "member-" + userId + "@example.com",
                      inviter.getId(),
                      ACCEPTED_AT.minusDays(1));
              UUID cardoInvitationId = UUID.randomUUID();
              invitation.registerCardoInvitation(
                  cardoInvitationId, userId, ACCEPTED_AT.plusDays(1));
              invitations.saveAndFlush(invitation);
              configureAdmissionContext(polity.getId());
              IdentityUser identity = identity(userId, invitation.getEmail());
              return new Fixture(
                  polity.getId(),
                  invitation.getId(),
                  cardoInvitationId,
                  identity,
                  new AuthenticatedUser(
                      identity.id(), identity.authorizationSubject(), identity.name()));
            });
  }

  private Invitation acceptedInvitation(Fixture fixture) {
    return new Invitation(
        fixture.cardoInvitationId(),
        fixture.invitationId(),
        fixture.polityId(),
        "polity:polity",
        fixture.identity().email(),
        fixture.identity().id(),
        UUID.randomUUID(),
        InvitationStatus.ACCEPTED,
        ACCEPTED_AT.plusDays(1),
        ACCEPTED_AT,
        null,
        ACCEPTED_AT.minusDays(1),
        ACCEPTED_AT);
  }

  private Membership membership(UUID polityId, UUID userId, String label) {
    return new Membership(
        polityId,
        userId,
        "subject-" + label + "-" + userId,
        label + "-" + userId + "@example.com",
        label,
        ACCEPTED_AT.minusDays(2),
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
        ACCEPTED_AT.minusDays(3),
        ACCEPTED_AT.minusDays(2));
  }

  private void configureAdmissionContext(UUID polityId) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Constitution", "Body", ACCEPTED_AT.minusDays(3));
    ReflectionTestUtils.setField(constitution, "id", UUID.randomUUID());
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Root", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    when(polityContext.constitution(polityId)).thenReturn(constitution);
    when(polityContext.rootJurisdiction(polityId)).thenReturn(jurisdiction);
  }

  private Publication acceptancePublication(UUID invitationId) {
    return jdbc.queryForObject(
        """
        select serialized_event, completion_date, last_resubmission_date
        from polity_events.event_publication
        where listener_id = ?
          and serialized_event like ?
        """,
        (result, rowNumber) ->
            new Publication(
                result.getString("serialized_event"),
                instant(result.getTimestamp("completion_date")),
                instant(result.getTimestamp("last_resubmission_date"))),
        ACCEPTANCE_LISTENER,
        "%" + invitationId + "%");
  }

  private long membershipCount(Fixture fixture) {
    return jdbc.queryForObject(
        """
        select count(*)
        from public.memberships
        where polity_id = ? and user_id = ?
        """,
        Long.class,
        fixture.polityId(),
        fixture.identity().id());
  }

  private void retryIncomplete() {
    new CardoInvitationRecovery(publications, Duration.ZERO).retryIncomplete();
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private record Fixture(
      UUID polityId,
      UUID invitationId,
      UUID cardoInvitationId,
      IdentityUser identity,
      AuthenticatedUser actor) {}

  private record Publication(
      String serializedEvent, Instant completionDate, Instant lastResubmissionDate) {}

  @TestConfiguration(proxyBeanMethods = false)
  static class DeterministicAsyncConfiguration {
    @Bean(name = "applicationTaskExecutor")
    @Primary
    SyncTaskExecutor applicationTaskExecutor() {
      return new SyncTaskExecutor();
    }

    @Bean
    RestClient.Builder restClientBuilder() {
      return RestClient.builder();
    }
  }
}
