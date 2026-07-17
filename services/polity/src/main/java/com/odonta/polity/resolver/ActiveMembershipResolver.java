package com.odonta.polity.resolver;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.repository.MembershipRepository;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveMembershipResolver {
  private final MembershipRepository memberships;

  public Membership resolve(UUID polityId, UUID userId) {
    return memberships
        .findEntityByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
        .orElseThrow(
            () ->
                ApiException.forbidden(
                    "polity_membership_required", "Active membership is required."));
  }

  public Membership resolveById(UUID polityId, UUID membershipId) {
    return memberships
        .findEntityById(membershipId)
        .filter(member -> member.getPolityId().equals(polityId))
        .filter(member -> member.getStatus() == MembershipStatus.ACTIVE)
        .orElseThrow(PolityResource.MEMBER::notFound);
  }
}
