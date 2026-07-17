package com.odonta.polity.integration.invite;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class CardoInvitationDispatchTest {

  @Test
  void stagesCreationAndAcceptanceAsApplicationEvents() {
    ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    CardoInvitationDispatch dispatch = new CardoInvitationDispatch(events);
    UUID invitationId = UUID.randomUUID();
    UUID inviterId = UUID.randomUUID();
    OffsetDateTime acceptedAt = OffsetDateTime.parse("2026-07-17T12:00:00Z");

    dispatch.stageCreation(invitationId, inviterId);
    dispatch.stageAcceptance(invitationId, acceptedAt);

    verify(events).publishEvent(new CardoInvitationCreationRequested(invitationId, inviterId));
    verify(events).publishEvent(new CardoInvitationAcceptanceRequested(invitationId, acceptedAt));
  }
}
