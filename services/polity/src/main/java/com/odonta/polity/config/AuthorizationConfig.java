package com.odonta.polity.config;

import com.odonta.polity.model.Polity;
import com.odonta.polity.repository.PolityRepository;
import io.github.lutzseverino.cardo.authorization.AuthorizationAdminClient;
import io.github.lutzseverino.cardo.authorization.grant.AuthorizationPlanConfiguration;
import io.github.lutzseverino.cardo.authorization.keycloak.KeycloakAuthorizationClient;
import io.github.lutzseverino.cardo.authorization.keycloak.KeycloakClientCredentialsTokenProvider;
import io.github.lutzseverino.cardo.authorization.schema.AuthorizationSchemaConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestClient;

@Configuration
@Import({AuthorizationPlanConfiguration.class, AuthorizationSchemaConfiguration.class})
@EntityScan(basePackageClasses = Polity.class)
@EnableJpaRepositories(basePackageClasses = PolityRepository.class)
public class AuthorizationConfig {
  @Bean
  @Primary
  KeycloakClientCredentialsTokenProvider keycloakClientCredentialsTokenProvider(
      KeycloakProperties keycloak, RestClient.Builder rest) {
    return new KeycloakClientCredentialsTokenProvider(
        keycloak.baseUrl(),
        keycloak.realm(),
        keycloak.clientId(),
        keycloak.clientSecret(),
        rest.clone());
  }

  @Bean
  KeycloakClientCredentialsTokenProvider polityCatalogTokens(
      KeycloakProperties keycloak, RestClient.Builder rest) {
    return new KeycloakClientCredentialsTokenProvider(
        keycloak.baseUrl(),
        keycloak.realm(),
        keycloak.catalogClientId(),
        keycloak.catalogClientSecret(),
        rest.clone());
  }

  @Bean
  KeycloakClientCredentialsTokenProvider polityRealmAdminTokens(
      KeycloakProperties keycloak, RestClient.Builder rest) {
    return new KeycloakClientCredentialsTokenProvider(
        keycloak.baseUrl(),
        keycloak.realm(),
        keycloak.realmAdminClientId(),
        keycloak.realmAdminClientSecret(),
        rest.clone());
  }

  @Bean
  AuthorizationAdminClient keycloakAuthorizationClient(
      KeycloakProperties keycloak,
      RestClient.Builder rest,
      @Qualifier("polityCatalogTokens") KeycloakClientCredentialsTokenProvider catalogTokens,
      @Qualifier("polityRealmAdminTokens") KeycloakClientCredentialsTokenProvider realmAdminTokens) {
    return new KeycloakAuthorizationClient(
        keycloak.baseUrl(),
        keycloak.realm(),
        keycloak.catalogClientId(),
        rest.clone(),
        catalogTokens::clientCredentialsToken,
        realmAdminTokens::clientCredentialsToken);
  }
}
