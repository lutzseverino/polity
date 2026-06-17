package com.odonta.polity.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.authorization.grant.GrantPlan;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import com.odonta.polity.model.PowerCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolityGrantPlannerTest {
  private final PolityGrantPlanner planner = new PolityGrantPlanner();

  @Test
  void polityResourceOnlyDefinesCoarseProductAccessActions() {
    assertThat(PolityResources.POLITY.actions())
        .containsExactly(PolityPermissions.READ, PolityPermissions.PARTICIPATE)
        .doesNotContain("manage-members", PowerCode.ADMIT_MEMBER.name());
  }

  @Test
  void founderGrantDoesNotEncodeConstitutionalPowers() {
    UUID polityId = UUID.randomUUID();

    GrantPlan plan = planner.founder("user:founder", polityId);

    assertThat(plan.authorityGrants()).isEmpty();
    assertThat(plan.resourceGrants()).hasSize(1);
    assertThat(plan.resourceGrants().getFirst().actions())
        .containsExactly(PolityPermissions.READ, PolityPermissions.PARTICIPATE)
        .doesNotContain("manage-members", PowerCode.ADMIT_MEMBER.name());
  }

  @Test
  void membershipGrantOnlyAllowsAccessAndParticipation() {
    UUID polityId = UUID.randomUUID();

    GrantPlan plan = planner.membership("user:member", polityId);

    assertThat(plan.authorityGrants()).isEmpty();
    assertThat(plan.resourceGrants()).hasSize(1);
    assertThat(plan.resourceGrants().getFirst().actions())
        .containsExactly(PolityPermissions.READ, PolityPermissions.PARTICIPATE)
        .doesNotContain("manage-members", PowerCode.ADMIT_MEMBER.name());
  }
}
