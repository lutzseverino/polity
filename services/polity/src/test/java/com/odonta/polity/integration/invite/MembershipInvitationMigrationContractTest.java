package com.odonta.polity.integration.invite;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class MembershipInvitationMigrationContractTest {

  @Test
  void legacyPendingInvitationsAreMadeTerminalSoTheyCanBeReissued() throws IOException {
    try (InputStream stream =
        Objects.requireNonNull(
            getClass()
                .getResourceAsStream(
                    "/db/migration/V3__membership_invitation_cardo_integration.sql"))) {
      String migration = new String(stream.readAllBytes(), UTF_8);

      assertThat(migration)
          .contains("'CANCELLED'::text")
          .contains("SET status = 'CANCELLED'")
          .contains("WHERE status = 'PENDING'")
          .contains("AND cardo_invitation_id IS NULL");
    }
  }
}
