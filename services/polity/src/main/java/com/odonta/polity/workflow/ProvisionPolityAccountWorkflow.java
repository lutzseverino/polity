package com.odonta.polity.workflow;

import com.odonta.polity.authorization.PolityGrantPlanner;
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

  @Transactional
  public void provision(AuthenticatedUser user) {
    grants.stage(grantPlanner.account(user.authorizationSubject()));
  }
}
