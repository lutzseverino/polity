package com.odonta.polity.service;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.PolityAccount;
import com.odonta.polity.repository.PolityAccountRepository;
import com.odonta.polity.result.PolityAccountResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolityAccountService {
  private final PolityAccountRepository accounts;
  private final GrantConvergenceService convergence;

  @Transactional(readOnly = true)
  public PolityAccountResult get(UUID userId) {
    PolityAccount account =
        accounts.findById(userId).orElseThrow(PolityResource.POLITY_ACCOUNT::notFound);
    return new PolityAccountResult(
        account.getUserId(), convergence.get(account.getGrantReceiptId()));
  }
}
