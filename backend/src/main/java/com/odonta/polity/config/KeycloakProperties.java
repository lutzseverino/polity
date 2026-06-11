package com.odonta.polity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "odonta.polity.keycloak")
public record KeycloakProperties(
    String baseUrl, String realm, String clientId, String clientSecret) {}
