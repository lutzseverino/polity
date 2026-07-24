package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.model.PolityAccount;
import com.odonta.polity.repository.PolityAccountRepository;
import com.odonta.polity.result.GrantConvergenceResult;
import com.odonta.polity.service.GrantConvergenceService;
import io.github.lutzseverino.cardo.authorization.grant.GrantPlan;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceipt;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceiptStatus;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProvisionPolityAccountWorkflowTest {
  private final Grants grants = mock(Grants.class);
  private final PolityAccountRepository accounts = mock(PolityAccountRepository.class);
  private final GrantConvergenceService convergence = mock(GrantConvergenceService.class);
  private final ProvisionPolityAccountWorkflow workflow =
      new ProvisionPolityAccountWorkflow(grants, new PolityGrantPlanner(), accounts, convergence);

  @Test
  void provisionPersistsTheReceiptWithTheAccountLifecycle() {
    UUID userId = UUID.randomUUID();
    UUID receiptId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, "subject-1", "Citizen");
    ArgumentCaptor<GrantPlan> plan = ArgumentCaptor.forClass(GrantPlan.class);
    ArgumentCaptor<PolityAccount> account = ArgumentCaptor.forClass(PolityAccount.class);
    when(accounts.findById(userId)).thenReturn(Optional.empty());
    GrantReceipt receipt = new GrantReceipt(receiptId, GrantReceiptStatus.PENDING, null);
    when(grants.stage(org.mockito.ArgumentMatchers.any())).thenReturn(receipt);

    var result = workflow.provision(user);

    verify(grants).stage(plan.capture());
    verify(accounts).lockProvisioning(userId);
    verify(accounts).saveAndFlush(account.capture());
    assertThat(plan.getValue().resourceGrants()).isEmpty();
    assertThat(plan.getValue().authorityGrants())
        .singleElement()
        .satisfies(
            grant -> {
              assertThat(grant.resourceServerClientId()).isEqualTo(PolityPermissions.CLIENT_ID);
              assertThat(grant.subject()).isEqualTo("subject-1");
              assertThat(grant.authorities()).containsExactly(PolityPermissions.POLITY_CREATE);
            });
    assertThat(account.getValue().getUserId()).isEqualTo(userId);
    assertThat(account.getValue().getAuthorizationSubject()).isEqualTo("subject-1");
    assertThat(account.getValue().getGrantReceiptId()).isEqualTo(receiptId);
    assertThat(result.created()).isTrue();
    assertThat(result.account().userId()).isEqualTo(userId);
    assertThat(result.account().grants().receiptId()).isEqualTo(receiptId);
    assertThat(result.account().grants().status()).isEqualTo(GrantReceiptStatus.PENDING);
  }

  @Test
  void repeatedProvisioningReturnsTheExistingDurableStateWithoutRestaging() {
    UUID userId = UUID.randomUUID();
    UUID receiptId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, "subject-1", "Citizen");
    PolityAccount account = new PolityAccount(userId, "subject-1", receiptId);
    when(accounts.findById(userId)).thenReturn(Optional.of(account));
    when(convergence.get(receiptId))
        .thenReturn(
            new GrantConvergenceResult(
                receiptId, GrantReceiptStatus.FAILED, "provider_application_failed"));

    var result = workflow.provision(user);

    verify(grants, never()).stage(org.mockito.ArgumentMatchers.any());
    verify(accounts).lockProvisioning(userId);
    verify(accounts, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    assertThat(result.created()).isFalse();
    assertThat(result.account().grants().status()).isEqualTo(GrantReceiptStatus.FAILED);
    assertThat(result.account().grants().failureCode()).isEqualTo("provider_application_failed");
  }
}
