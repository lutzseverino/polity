package com.odonta.polity.integration.invite;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class CardoInvitationIntegrationContractTest {

  @Test
  void remoteListenersExplicitlyRunWithoutPolityTransactions() throws Exception {
    assertNoTransaction("create", CardoInvitationCreationRequested.class);
    assertNoTransaction("accept", CardoInvitationAcceptanceRequested.class);
  }

  @Test
  void cardoRegistrationUsesItsOwnLocalTransaction() throws Exception {
    Method registration =
        CardoInvitationState.class.getDeclaredMethod(
            "register",
            java.util.UUID.class,
            java.util.UUID.class,
            java.util.UUID.class,
            java.time.OffsetDateTime.class);

    assertThat(registration.getAnnotation(Transactional.class).propagation())
        .isEqualTo(Propagation.REQUIRES_NEW);
  }

  private void assertNoTransaction(String methodName, Class<?> eventType) throws Exception {
    Method listener = CardoInvitationListener.class.getDeclaredMethod(methodName, eventType);

    assertThat(listener.getAnnotation(ApplicationModuleListener.class).propagation())
        .isEqualTo(Propagation.NOT_SUPPORTED);
  }
}
