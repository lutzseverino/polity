package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.repository.MembershipRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipReader {
  private final MembershipRepository memberships;

  public Membership active(UUID polityId, UUID userId) {
    return memberships
        .findByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
        .orElseThrow(
            () ->
                ApiException.forbidden(
                    "polity_membership_required", "Active membership is required."));
  }
}
