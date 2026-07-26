package com.odonta.polity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class MembershipAccessMigrationIntegrationTest {
  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Test
  void preservesLegacyRowsAndRejectsContradictoryNonNullAcceptanceOutcomes() throws SQLException {
    flyway().target("5").load().migrate();

    UUID polityId = UUID.randomUUID();
    UUID inviterId = UUID.randomUUID();
    UUID pendingId = UUID.randomUUID();
    UUID acceptedId = UUID.randomUUID();
    UUID cancelledId = UUID.randomUUID();
    seedPolityAndInviter(polityId, inviterId);
    insertInvitation(pendingId, polityId, inviterId, "pending@example.com", "PENDING");
    insertInvitation(acceptedId, polityId, inviterId, "accepted@example.com", "ACCEPTED");
    insertInvitation(cancelledId, polityId, inviterId, "cancelled@example.com", "CANCELLED");

    flyway().load().migrate();

    Map<String, String> legacyStates = acceptanceStates();
    assertThat(legacyStates).containsOnlyKeys("PENDING", "ACCEPTED", "CANCELLED");
    assertThat(legacyStates.values()).containsOnlyNulls();

    setAcceptance(pendingId, "PENDING", "REQUESTED");
    setAcceptance(acceptedId, "ACCEPTED", "COMPLETED");
    setAcceptance(cancelledId, "CANCELLED", "FAILED");

    Stream.of(
            Map.entry("PENDING", "COMPLETED"),
            Map.entry("PENDING", "FAILED"),
            Map.entry("ACCEPTED", "REQUESTED"),
            Map.entry("ACCEPTED", "FAILED"),
            Map.entry("CANCELLED", "REQUESTED"),
            Map.entry("CANCELLED", "COMPLETED"))
        .forEach(
            contradiction ->
                assertThatThrownBy(
                        () ->
                            setAcceptance(
                                UUID.randomUUID(),
                                contradiction.getKey(),
                                contradiction.getValue()))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("membership_invitations_acceptance_lifecycle_check"));
  }

  private void setAcceptance(UUID invitationId, String status, String acceptanceStatus)
      throws SQLException {
    if (!exists(invitationId)) {
      UUID polityId = UUID.randomUUID();
      UUID inviterId = UUID.randomUUID();
      seedPolityAndInviter(polityId, inviterId);
      insertInvitation(invitationId, polityId, inviterId, invitationId + "@example.com", "PENDING");
    }
    boolean requested = "REQUESTED".equals(acceptanceStatus);
    boolean failed = "FAILED".equals(acceptanceStatus);
    try (Connection connection = connection();
        PreparedStatement update =
            connection.prepareStatement(
                """
                update public.membership_invitations
                set status = ?,
                    acceptance_status = ?,
                    acceptance_requested_at = current_timestamp,
                    acceptance_completed_at = case when ? then null else current_timestamp end,
                    acceptance_failure_code = case when ? then 'terminal_failure' else null end
                where id = ?
                """)) {
      update.setString(1, status);
      update.setString(2, acceptanceStatus);
      update.setBoolean(3, requested);
      update.setBoolean(4, failed);
      update.setObject(5, invitationId);
      assertThat(update.executeUpdate()).isOne();
    }
  }

  private boolean exists(UUID invitationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement select =
            connection.prepareStatement(
                "select exists(select 1 from public.membership_invitations where id = ?)")) {
      select.setObject(1, invitationId);
      try (ResultSet result = select.executeQuery()) {
        result.next();
        return result.getBoolean(1);
      }
    }
  }

  private Map<String, String> acceptanceStates() throws SQLException {
    try (Connection connection = connection();
        PreparedStatement select =
            connection.prepareStatement(
                """
                select status, acceptance_status
                from public.membership_invitations
                order by status
                """);
        ResultSet result = select.executeQuery()) {
      Map<String, String> states = new java.util.LinkedHashMap<>();
      while (result.next()) {
        states.put(result.getString(1), result.getString(2));
      }
      return states;
    }
  }

  private void seedPolityAndInviter(UUID polityId, UUID inviterId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement polity =
            connection.prepareStatement(
                """
                insert into public.polities (id, founder_id, name, slug)
                values (?, ?, ?, ?)
                """);
        PreparedStatement inviter =
            connection.prepareStatement(
                """
                insert into public.memberships
                  (id, polity_id, user_id, authorization_subject, email, display_name,
                   status, admitted_at)
                values (?, ?, ?, ?, ?, 'Inviter', 'ACTIVE', current_timestamp)
                """)) {
      polity.setObject(1, polityId);
      polity.setObject(2, UUID.randomUUID());
      polity.setString(3, "Migration " + polityId);
      polity.setString(4, "migration-" + polityId);
      polity.executeUpdate();

      inviter.setObject(1, inviterId);
      inviter.setObject(2, polityId);
      inviter.setObject(3, UUID.randomUUID());
      inviter.setString(4, "subject-" + inviterId);
      inviter.setString(5, inviterId + "@example.com");
      inviter.executeUpdate();
    }
  }

  private void insertInvitation(
      UUID invitationId, UUID polityId, UUID inviterId, String email, String status)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement insert =
            connection.prepareStatement(
                """
                insert into public.membership_invitations
                  (id, polity_id, invited_user_id, cardo_invitation_id, cardo_expires_at,
                   email, invited_by, status, invited_at, responded_at)
                values (?, ?, ?, ?, current_timestamp + interval '1 day',
                        ?, ?, ?, current_timestamp,
                        case when ? = 'PENDING' then null else current_timestamp end)
                """)) {
      insert.setObject(1, invitationId);
      insert.setObject(2, polityId);
      insert.setObject(3, UUID.randomUUID());
      insert.setObject(4, UUID.randomUUID());
      insert.setString(5, email);
      insert.setObject(6, inviterId);
      insert.setString(7, status);
      insert.setString(8, status);
      insert.executeUpdate();
    }
  }

  private static org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
    return Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .table("flyway_schema_history_membership_access");
  }

  private static Connection connection() throws SQLException {
    return java.sql.DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }
}
