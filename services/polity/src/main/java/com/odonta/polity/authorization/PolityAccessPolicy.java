package com.odonta.polity.authorization;

import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.service.MembershipService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolityAccessPolicy {
  private final MembershipService memberships;
  private final PolityRepository polities;

  public boolean isPublic(UUID polityId) {
    return polities.existsByIdAndVisibility(polityId, PolityVisibility.PUBLIC);
  }

  public void requireReadable(UUID polityId, UUID userId) {
    if (isPublic(polityId)) {
      return;
    }
    memberships.requireActive(polityId, userId);
  }
}
