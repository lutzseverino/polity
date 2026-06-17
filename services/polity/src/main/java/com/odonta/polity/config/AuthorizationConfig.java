package com.odonta.polity.config;

import com.odonta.authorization.AuthorizationAdminClient;
import com.odonta.authorization.grant.AuthorizationPlanConfiguration;
import com.odonta.authorization.keycloak.KeycloakAuthorizationClient;
import com.odonta.authorization.keycloak.KeycloakClientCredentialsTokenProvider;
import com.odonta.authorization.schema.AuthorizationSchemaConfiguration;
import com.odonta.polity.model.Polity;
import com.odonta.polity.repository.PolityRepository;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestClient;

@Configuration
@Import({AuthorizationPlanConfiguration.class, AuthorizationSchemaConfiguration.class})
@EntityScan(basePackageClasses = Polity.class)
@EnableJpaRepositories(basePackageClasses = PolityRepository.class)
public class AuthorizationConfig {
  @Bean
  KeycloakClientCredentialsTokenProvider keycloakClientCredentialsTokenProvider(
      KeycloakProperties keycloak, RestClient.Builder rest) {
    return new KeycloakClientCredentialsTokenProvider(
        keycloak.baseUrl(), keycloak.realm(), keycloak.clientId(), keycloak.clientSecret(), rest);
  }

  @Bean
  AuthorizationAdminClient keycloakAuthorizationClient(
      KeycloakProperties keycloak,
      RestClient.Builder rest,
      KeycloakClientCredentialsTokenProvider tokens) {
    return new KeycloakAuthorizationClient(
        keycloak.baseUrl(), keycloak.realm(), rest, tokens::clientCredentialsToken);
  }
}
