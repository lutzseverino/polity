package com.odonta.polity.authorization;

import com.odonta.authorization.grant.GrantPlan;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PolityGrantPlanner {
  public GrantPlan account(String subject) {
    return GrantPlan.builder()
        .grantAuthorities(
            subject, PolityPermissions.CLIENT_ID, List.of(PolityPermissions.POLITY_CREATE))
        .build();
  }

  public GrantPlan membership(String subject, UUID polityId) {
    return GrantPlan.builder()
        .grantActions(
            subject,
            PolityResources.POLITY.resource(polityId),
            List.of(PolityPermissions.READ, PolityPermissions.PARTICIPATE))
        .build();
  }
}
