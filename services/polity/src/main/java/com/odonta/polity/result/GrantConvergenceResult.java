package com.odonta.polity.result;

import io.github.lutzseverino.cardo.authorization.grant.GrantReceipt;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceiptStatus;
import java.util.UUID;

public record GrantConvergenceResult(
    UUID receiptId, GrantReceiptStatus status, String failureCode) {

  public static GrantConvergenceResult from(GrantReceipt receipt) {
    return new GrantConvergenceResult(receipt.id(), receipt.status(), receipt.failureCode());
  }
}
