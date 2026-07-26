package com.odonta.polity.result;

import io.github.lutzseverino.cardo.authorization.grant.GrantReceipt;
import java.util.UUID;

public record MembershipAccessConvergenceResult(
    UUID membershipId,
    UUID receiptId,
    MembershipAccessConvergenceStatus status,
    String failureCode) {

  public static MembershipAccessConvergenceResult legacy(UUID membershipId) {
    return new MembershipAccessConvergenceResult(
        membershipId, null, MembershipAccessConvergenceStatus.LEGACY, null);
  }

  public static MembershipAccessConvergenceResult from(UUID membershipId, GrantReceipt receipt) {
    return new MembershipAccessConvergenceResult(
        membershipId,
        receipt.id(),
        MembershipAccessConvergenceStatus.valueOf(receipt.status().name()),
        receipt.failureCode());
  }
}
