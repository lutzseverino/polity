package com.odonta.polity.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import com.odonta.polity.model.PowerCode;
import io.github.lutzseverino.cardo.authorization.grant.GrantPlan;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolityGrantPlannerTest {
  private final PolityGrantPlanner planner = new PolityGrantPlanner();

  @Test
  void accountGrantAllowsPublicPolityCreation() {
    GrantPlan plan = planner.account("user:founder");

    assertThat(plan.resourceGrants()).isEmpty();
    assertThat(plan.authorityGrants()).hasSize(1);
    assertThat(plan.authorityGrants().getFirst().resourceServerClientId())
        .isEqualTo(PolityPermissions.CLIENT_ID);
    assertThat(plan.authorityGrants().getFirst().authorities())
        .containsExactly(PolityPermissions.POLITY_CREATE);
  }

  @Test
  void polityResourceOnlyDefinesCoarseProductAccessActions() {
    assertThat(PolityResources.POLITY.actions())
        .containsExactly(PolityPermissions.READ, PolityPermissions.PARTICIPATE)
        .doesNotContain("manage-members", PowerCode.ADMIT_MEMBER.name());
  }

  @Test
  void foundingMemberGrantDoesNotEncodeConstitutionalPowers() {
    UUID polityId = UUID.randomUUID();

    GrantPlan plan = planner.membership("user:founder", polityId);

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
