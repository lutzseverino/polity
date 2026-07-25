package com.odonta.polity.service;

import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.result.MembershipAccessConvergenceResult;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceipt;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipAccessService {
  private final Grants grants;
  private final MembershipRepository memberships;
  private final PolityGrantPlanner planner;

  @Transactional(readOnly = true)
  public MembershipAccessConvergenceResult get(UUID polityId, UUID userId) {
    return result(requireActive(polityId, userId));
  }

  @Transactional
  public MembershipAccessConvergenceResult reconcile(UUID polityId, UUID userId) {
    Membership membership =
        memberships
            .findEntityByPolityIdAndUserIdForUpdate(polityId, userId)
            .filter(candidate -> candidate.getStatus() == MembershipStatus.ACTIVE)
            .orElseThrow(this::membershipRequired);
    if (membership.getGrantReceiptId() == null) {
      GrantReceipt receipt =
          grants.stage(planner.membership(membership.getAuthorizationSubject(), polityId));
      membership.retainGrantReceipt(receipt.id());
      memberships.saveAndFlush(membership);
    }
    return result(membership);
  }

  private Membership requireActive(UUID polityId, UUID userId) {
    return memberships
        .findEntityByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
        .orElseThrow(this::membershipRequired);
  }

  private MembershipAccessConvergenceResult result(Membership membership) {
    if (membership.getGrantReceiptId() == null) {
      return MembershipAccessConvergenceResult.legacy(membership.getId());
    }
    GrantReceipt receipt =
        grants
            .find(membership.getGrantReceiptId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Unknown stored grant receipt: " + membership.getGrantReceiptId()));
    return MembershipAccessConvergenceResult.from(membership.getId(), receipt);
  }

  private ApiException membershipRequired() {
    return ApiException.forbidden("polity_membership_required", "Active membership is required.");
  }
}
