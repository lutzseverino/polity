package com.odonta.polity.service;

import com.odonta.polity.result.GrantConvergenceResult;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GrantConvergenceService {
  private final Grants grants;

  @Transactional(readOnly = true)
  public GrantConvergenceResult get(UUID receiptId) {
    return grants
        .find(receiptId)
        .map(GrantConvergenceResult::from)
        .orElseThrow(() -> new IllegalStateException("Unknown stored grant receipt: " + receiptId));
  }
}
