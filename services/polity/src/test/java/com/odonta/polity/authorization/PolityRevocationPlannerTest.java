package com.odonta.polity.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import io.github.lutzseverino.cardo.authorization.grant.RevocationPlan;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolityRevocationPlannerTest {
  private final PolityRevocationPlanner planner = new PolityRevocationPlanner();

  @Test
  void membershipRevocationRemovesAccessAndParticipation() {
    UUID polityId = UUID.randomUUID();

    RevocationPlan plan = planner.membership("user:member", polityId);

    assertThat(plan.authorityRevocations()).isEmpty();
    assertThat(plan.resourceRevocations()).hasSize(1);
    assertThat(plan.resourceRevocations().getFirst().resourceName())
        .isEqualTo(PolityResources.POLITY.resource(polityId).name());
    assertThat(plan.resourceRevocations().getFirst().actions())
        .containsExactly(PolityPermissions.READ, PolityPermissions.PARTICIPATE);
  }

  @Test
  void participationRevocationPreservesReadAccess() {
    UUID polityId = UUID.randomUUID();

    RevocationPlan plan = planner.participation("user:member", polityId);

    assertThat(plan.authorityRevocations()).isEmpty();
    assertThat(plan.resourceRevocations()).hasSize(1);
    assertThat(plan.resourceRevocations().getFirst().resourceName())
        .isEqualTo(PolityResources.POLITY.resource(polityId).name());
    assertThat(plan.resourceRevocations().getFirst().actions())
        .containsExactly(PolityPermissions.PARTICIPATE);
  }
}
