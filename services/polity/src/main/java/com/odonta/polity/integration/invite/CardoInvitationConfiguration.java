package com.odonta.polity.integration.invite;

import com.odonta.polity.config.MembershipInvitationProperties;
import com.odonta.polity.repository.MembershipInvitationRepository;
import io.github.lutzseverino.cardo.invite.client.InvitationsClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration(proxyBeanMethods = false)
class CardoInvitationConfiguration {

  @Bean
  CardoInvitationState cardoInvitationState(MembershipInvitationRepository invitations) {
    return new CardoInvitationState(invitations);
  }

  @Bean
  CardoInvitationProcessor cardoInvitationProcessor(
      InvitationsClient client,
      MembershipInvitationRepository invitations,
      MembershipInvitationProperties properties,
      CardoInvitationState state) {
    return new CardoInvitationProcessor(client, invitations, properties, state);
  }

  @Bean
  CardoInvitationListener cardoInvitationListener(CardoInvitationProcessor processor) {
    return new CardoInvitationListener(processor);
  }

  @Bean
  CardoInvitationRecovery cardoInvitationRecovery(
      IncompleteEventPublications publications,
      @Value("${polity.membership-invitations.retry-delay:PT1M}") Duration retryDelay) {
    return new CardoInvitationRecovery(publications, retryDelay);
  }
}
