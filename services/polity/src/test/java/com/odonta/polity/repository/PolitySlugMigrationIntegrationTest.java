package com.odonta.polity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class PolitySlugMigrationIntegrationTest {
  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Test
  void backfillsSlugsWithTheRuntimeNormalizationAndRouteReservationPolicy() throws SQLException {
    flyway().target("3").load().migrate();

    try (Connection connection = connection();
        PreparedStatement insert =
            connection.prepareStatement(
                "insert into public.polities (id, founder_id, name) values (?, ?, ?)")) {
      insertPolity(insert, "11111111-1111-4111-8111-111111111111", "Święto");
      insertPolity(insert, "22222222-2222-4222-8222-222222222222", "Święto");
      insertPolity(
          insert, "33333333-3333-4333-8333-333333333333", "44444444-4444-4444-8444-444444444444");
    }

    flyway().load().migrate();

    Map<String, String> slugs = new LinkedHashMap<>();
    try (Connection connection = connection();
        Statement statement = connection.createStatement();
        ResultSet result =
            statement.executeQuery("select name, slug from public.polities order by id")) {
      while (result.next()) {
        slugs.put(result.getString("name") + slugs.size(), result.getString("slug"));
      }
    }

    assertThat(slugs.values())
        .containsExactly("swieto", "swieto-2", "44444444-4444-4444-8444-444444444444-polity");
  }

  private static org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
    return Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .table("flyway_schema_history_polity");
  }

  private static Connection connection() throws SQLException {
    return java.sql.DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static void insertPolity(PreparedStatement insert, String id, String name)
      throws SQLException {
    insert.setObject(1, UUID.fromString(id));
    insert.setObject(2, UUID.randomUUID());
    insert.setString(3, name);
    insert.executeUpdate();
  }
}
