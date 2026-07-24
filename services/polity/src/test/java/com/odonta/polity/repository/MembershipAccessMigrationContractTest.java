package com.odonta.polity.repository;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class MembershipAccessMigrationContractTest {

  @Test
  void migrationRetainsLegacyMembershipsAndDurableAcceptanceOutcomes() throws IOException {
    try (InputStream stream =
        Objects.requireNonNull(
            getClass()
                .getResourceAsStream("/db/migration/V6__membership_access_convergence.sql"))) {
      String migration = new String(stream.readAllBytes(), UTF_8);

      assertThat(migration)
          .contains("ADD COLUMN grant_receipt_id UUID")
          .doesNotContain("grant_receipt_id UUID NOT NULL")
          .contains("ADD COLUMN acceptance_requested_at TIMESTAMP WITH TIME ZONE")
          .contains("'REQUESTED'::text, 'COMPLETED'::text, 'FAILED'::text")
          .contains("membership_invitations_acceptance_state_check")
          .contains("membership_invitations_acceptance_lifecycle_check")
          .contains("status = 'PENDING' AND acceptance_status = 'REQUESTED'")
          .contains("status = 'ACCEPTED' AND acceptance_status = 'COMPLETED'")
          .contains("status = 'CANCELLED' AND acceptance_status = 'FAILED'");
    }
  }
}
