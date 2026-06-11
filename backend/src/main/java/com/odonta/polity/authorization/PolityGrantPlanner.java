package com.odonta.polity.authorization;

import com.odonta.authorization.grant.GrantPlan;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PolityGrantPlanner {
  public GrantPlan founder(String subject, UUID polityId) {
    return GrantPlan.builder()
        .grantFullAccess(subject, PolityResources.POLITY.resource(polityId))
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
