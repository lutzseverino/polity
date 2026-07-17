package com.odonta.polity.integration.invite;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CardoInvitationDispatch {
  private final ApplicationEventPublisher events;

  @Transactional(propagation = Propagation.MANDATORY)
  public void stageCreation(UUID invitationId, UUID invitedByUserId) {
    events.publishEvent(
        new CardoInvitationCreationRequested(
            Objects.requireNonNull(invitationId), Objects.requireNonNull(invitedByUserId)));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void stageAcceptance(UUID invitationId, OffsetDateTime acceptedAt) {
    events.publishEvent(
        new CardoInvitationAcceptanceRequested(
            Objects.requireNonNull(invitationId), Objects.requireNonNull(acceptedAt)));
  }
}
