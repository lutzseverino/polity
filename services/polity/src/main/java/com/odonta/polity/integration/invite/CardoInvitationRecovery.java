package com.odonta.polity.integration.invite;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
class CardoInvitationRecovery {
  private final IncompleteEventPublications publications;
  private final Duration retryDelay;

  @Scheduled(fixedDelayString = "${polity.membership-invitations.retry-delay:PT1M}")
  void retryIncomplete() {
    Instant cutoff = Instant.now().minus(retryDelay);
    publications.resubmitIncompletePublications(
        publication ->
            publication.getPublicationDate().isBefore(cutoff)
                && (publication.getEvent() instanceof CardoInvitationCreationRequested
                    || publication.getEvent() instanceof CardoInvitationAcceptanceRequested));
  }
}
