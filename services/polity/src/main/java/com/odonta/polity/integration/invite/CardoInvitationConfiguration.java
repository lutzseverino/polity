package com.odonta.polity.integration.invite;

import com.odonta.polity.config.MembershipInvitationProperties;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.workflow.CompleteMembershipInvitationWorkflow;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
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
  CardoInvitationState cardoInvitationState(
      java.time.Clock clock, MembershipInvitationRepository invitations) {
    return new CardoInvitationState(clock, invitations);
  }

  @Bean
  CardoInvitationProcessor cardoInvitationProcessor(
      InvitationsClient client,
      IdentityUsersClient identityUsers,
      MembershipInvitationRepository invitations,
      MembershipInvitationProperties properties,
      CardoInvitationState state,
      CompleteMembershipInvitationWorkflow completion) {
    return new CardoInvitationProcessor(
        client, identityUsers, invitations, properties, state, completion);
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
