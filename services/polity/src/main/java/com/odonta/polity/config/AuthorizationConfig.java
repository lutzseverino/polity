package com.odonta.polity.config;

import com.odonta.polity.model.Polity;
import com.odonta.polity.repository.PolityRepository;
import io.github.lutzseverino.cardo.authorization.AuthorizationAdminClient;
import io.github.lutzseverino.cardo.authorization.grant.AuthorizationPlanConfiguration;
import io.github.lutzseverino.cardo.authorization.keycloak.KeycloakAuthorizationClient;
import io.github.lutzseverino.cardo.authorization.keycloak.KeycloakClientCredentialsTokenProvider;
import io.github.lutzseverino.cardo.authorization.schema.AuthorizationSchemaConfiguration;
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
