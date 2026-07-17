package com.odonta.polity.integration.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;

class CardoInvitationRecoveryTest {

  @Test
  void retriesOnlyCardoInvitationIntegrationPublications() {
    IncompleteEventPublications publications = mock(IncompleteEventPublications.class);
    CardoInvitationRecovery recovery =
        new CardoInvitationRecovery(publications, Duration.ofMinutes(1));

    recovery.retryIncomplete();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Predicate<EventPublication>> filter = ArgumentCaptor.forClass(Predicate.class);
    verify(publications).resubmitIncompletePublications(filter.capture());
    assertThat(
            filter
                .getValue()
                .test(
                    publication(
                        new CardoInvitationCreationRequested(UUID.randomUUID(), UUID.randomUUID()),
                        Instant.EPOCH)))
        .isTrue();
    assertThat(
            filter
                .getValue()
                .test(
                    publication(
                        new CardoInvitationAcceptanceRequested(
                            UUID.randomUUID(),
                            java.time.OffsetDateTime.parse("2026-07-17T12:00:00Z")),
                        Instant.EPOCH)))
        .isTrue();
    assertThat(filter.getValue())
        .rejects(
            publication(
                new CardoInvitationCreationRequested(UUID.randomUUID(), UUID.randomUUID()),
                Instant.now()),
            publication("unrelated", Instant.EPOCH));
  }

  private EventPublication publication(Object event, Instant publicationDate) {
    EventPublication publication = mock(EventPublication.class);
    when(publication.getEvent()).thenReturn(event);
    when(publication.getPublicationDate()).thenReturn(publicationDate);
    return publication;
  }
}
