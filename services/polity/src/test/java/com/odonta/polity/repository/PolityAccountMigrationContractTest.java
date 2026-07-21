package com.odonta.polity.repository;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class PolityAccountMigrationContractTest {

  @Test
  void accountLifecycleOwnsExactlyOneDurableGrantReceipt() throws IOException {
    try (InputStream stream =
        Objects.requireNonNull(
            getClass()
                .getResourceAsStream("/db/migration/V5__polity_account_grant_convergence.sql"))) {
      String migration = new String(stream.readAllBytes(), UTF_8);

      assertThat(migration)
          .contains("CREATE TABLE public.polity_accounts")
          .contains("user_id UUID PRIMARY KEY")
          .contains("authorization_subject TEXT NOT NULL UNIQUE")
          .contains("grant_receipt_id UUID NOT NULL UNIQUE");
    }
  }
}
