package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.lutzseverino.cardo.authorization.grant.GrantReceipt;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceiptStatus;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GrantConvergenceServiceTest {
  private final Grants grants = mock(Grants.class);
  private final GrantConvergenceService convergence = new GrantConvergenceService(grants);

  @Test
  void exposesEveryDurableReceiptStateAndStableFailureCode() {
    UUID pendingId = UUID.randomUUID();
    UUID appliedId = UUID.randomUUID();
    UUID failedId = UUID.randomUUID();
    when(grants.find(pendingId))
        .thenReturn(Optional.of(new GrantReceipt(pendingId, GrantReceiptStatus.PENDING, null)));
    when(grants.find(appliedId))
        .thenReturn(Optional.of(new GrantReceipt(appliedId, GrantReceiptStatus.APPLIED, null)));
    when(grants.find(failedId))
        .thenReturn(
            Optional.of(
                new GrantReceipt(
                    failedId, GrantReceiptStatus.FAILED, "provider_application_failed")));

    assertThat(convergence.get(pendingId).status()).isEqualTo(GrantReceiptStatus.PENDING);
    assertThat(convergence.get(appliedId).status()).isEqualTo(GrantReceiptStatus.APPLIED);
    assertThat(convergence.get(failedId))
        .satisfies(
            result -> {
              assertThat(result.status()).isEqualTo(GrantReceiptStatus.FAILED);
              assertThat(result.failureCode()).isEqualTo("provider_application_failed");
            });
  }

  @Test
  void rejectsAStoredReceiptIdentifierThatAuthorizationDoesNotKnow() {
    UUID receiptId = UUID.randomUUID();
    when(grants.find(receiptId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> convergence.get(receiptId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unknown stored grant receipt: " + receiptId);
  }
}
