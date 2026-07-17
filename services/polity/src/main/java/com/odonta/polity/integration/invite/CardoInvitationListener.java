package com.odonta.polity.integration.invite;

import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.annotation.Propagation;

@RequiredArgsConstructor
class CardoInvitationListener {
  private final CardoInvitationProcessor processor;

  @ApplicationModuleListener(
      id = "polity.cardo-invitation-creation",
      propagation = Propagation.NOT_SUPPORTED)
  void create(CardoInvitationCreationRequested request) {
    processor.create(request);
  }

  @ApplicationModuleListener(
      id = "polity.cardo-invitation-acceptance",
      propagation = Propagation.NOT_SUPPORTED)
  void accept(CardoInvitationAcceptanceRequested request) {
    processor.accept(request);
  }
}
