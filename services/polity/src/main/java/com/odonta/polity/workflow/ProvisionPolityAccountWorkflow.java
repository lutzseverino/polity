package com.odonta.polity.workflow;

import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.model.PolityAccount;
import com.odonta.polity.repository.PolityAccountRepository;
import com.odonta.polity.result.GrantConvergenceResult;
import com.odonta.polity.result.PolityAccountResult;
import com.odonta.polity.result.ProvisionPolityAccountResult;
import com.odonta.polity.service.GrantConvergenceService;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProvisionPolityAccountWorkflow {
  private final Grants grants;
  private final PolityGrantPlanner grantPlanner;
  private final PolityAccountRepository accounts;
  private final GrantConvergenceService convergence;

  @Transactional
  public ProvisionPolityAccountResult provision(AuthenticatedUser user) {
    accounts.lockProvisioning(user.id());
    return accounts
        .findById(user.id())
        .map(account -> existing(account))
        .orElseGet(() -> provisionNew(user));
  }

  private ProvisionPolityAccountResult provisionNew(AuthenticatedUser user) {
    var receipt = grants.stage(grantPlanner.account(user.authorizationSubject()));
    accounts.saveAndFlush(new PolityAccount(user.id(), user.authorizationSubject(), receipt.id()));
    return new ProvisionPolityAccountResult(
        new PolityAccountResult(user.id(), GrantConvergenceResult.from(receipt)), true);
  }

  private ProvisionPolityAccountResult existing(PolityAccount account) {
    return new ProvisionPolityAccountResult(
        new PolityAccountResult(account.getUserId(), convergence.get(account.getGrantReceiptId())),
        false);
  }
}
