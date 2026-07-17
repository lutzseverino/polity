package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityGrantPlanner;
import io.github.lutzseverino.cardo.authorization.grant.GrantPlan;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProvisionPolityAccountWorkflowTest {
  private final Grants grants = mock(Grants.class);
  private final ProvisionPolityAccountWorkflow workflow =
      new ProvisionPolityAccountWorkflow(grants, new PolityGrantPlanner());

  @Test
  void provisionStagesCreationAuthority() {
    AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "subject-1", "Citizen");
    ArgumentCaptor<GrantPlan> plan = ArgumentCaptor.forClass(GrantPlan.class);

    workflow.provision(user);

    verify(grants).stage(plan.capture());
    assertThat(plan.getValue().resourceGrants()).isEmpty();
    assertThat(plan.getValue().authorityGrants())
        .singleElement()
        .satisfies(
            grant -> {
              assertThat(grant.resourceServerClientId()).isEqualTo(PolityPermissions.CLIENT_ID);
              assertThat(grant.subject()).isEqualTo("subject-1");
              assertThat(grant.authorities()).containsExactly(PolityPermissions.POLITY_CREATE);
            });
  }
}
