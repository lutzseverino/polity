package com.odonta.polity.authorization;

import com.odonta.authorization.grant.RevocationPlan;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PolityRevocationPlanner {
  public RevocationPlan membership(String subject, UUID polityId) {
    return RevocationPlan.builder()
        .revokeFullAccess(subject, PolityResources.POLITY.resource(polityId))
        .build();
  }

  public RevocationPlan participation(String subject, UUID polityId) {
    return RevocationPlan.builder()
        .revokeActions(
            subject,
            PolityResources.POLITY.resource(polityId),
            List.of(PolityPermissions.PARTICIPATE))
        .build();
  }
}
