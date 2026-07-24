package com.odonta.polity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.lutzseverino.cardo.authorization.keycloak.KeycloakAuthorizationClient;
import io.github.lutzseverino.cardo.authorization.keycloak.KeycloakClientCredentialsTokenProvider;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class AuthorizationConfigTest {

  @Test
  void authorizationUsesDistinctOutboundCatalogAndRealmAdminCredentials() {
    AuthorizationConfig config = new AuthorizationConfig();
    KeycloakProperties properties =
        new KeycloakProperties(
            "https://keycloak.example.com",
            "odonta",
            "polity-outbound",
            "outbound-secret",
            "polity",
            "catalog-secret",
            "polity-realm-admin",
            "realm-admin-secret");
    RestClient.Builder rest = RestClient.builder();

    KeycloakClientCredentialsTokenProvider outbound =
        config.keycloakClientCredentialsTokenProvider(properties, rest.clone());
    KeycloakClientCredentialsTokenProvider catalog =
        config.polityCatalogTokens(properties, rest.clone());
    KeycloakClientCredentialsTokenProvider realmAdmin =
        config.polityRealmAdminTokens(properties, rest.clone());

    assertThat(clientId(outbound)).isEqualTo("polity-outbound");
    assertThat(clientId(catalog)).isEqualTo("polity");
    assertThat(clientId(realmAdmin)).isEqualTo("polity-realm-admin");

    KeycloakClientCredentialsTokenProvider catalogTokens =
        mock(KeycloakClientCredentialsTokenProvider.class);
    KeycloakClientCredentialsTokenProvider realmAdminTokens =
        mock(KeycloakClientCredentialsTokenProvider.class);
    when(catalogTokens.clientCredentialsToken()).thenReturn("catalog-pat");
    when(realmAdminTokens.clientCredentialsToken()).thenReturn("realm-admin-token");
    KeycloakAuthorizationClient authorization =
        (KeycloakAuthorizationClient)
            config.keycloakAuthorizationClient(
                properties, rest.clone(), catalogTokens, realmAdminTokens);

    assertThat(ReflectionTestUtils.getField(authorization, "resourceServerClientId"))
        .isEqualTo("polity");
    assertThat(token(authorization, "protectionApiToken")).isEqualTo("catalog-pat");
    assertThat(token(authorization, "realmAdminToken")).isEqualTo("realm-admin-token");
  }

  private String clientId(KeycloakClientCredentialsTokenProvider provider) {
    return (String) ReflectionTestUtils.getField(provider, "clientId");
  }

  @SuppressWarnings("unchecked")
  private String token(KeycloakAuthorizationClient client, String field) {
    return ((Supplier<String>) ReflectionTestUtils.getField(client, field)).get();
  }
}
