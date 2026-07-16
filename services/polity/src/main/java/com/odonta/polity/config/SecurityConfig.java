package com.odonta.polity.config;

import io.github.lutzseverino.cardo.authorization.keycloak.KeycloakAuthoritiesConverter;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import io.github.lutzseverino.cardo.authorization.spring.ResourcePermissionEvaluator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  KeycloakAuthoritiesConverter keycloakAuthoritiesConverter() {
    return new KeycloakAuthoritiesConverter();
  }

  @Bean
  PermissionEvaluator permissionEvaluator() {
    return new ResourcePermissionEvaluator();
  }

  @Bean
  AuthenticatedUserReader authenticatedUserReader() {
    return new AuthenticatedUserReader();
  }

  @Bean
  MethodSecurityExpressionHandler methodSecurityExpressionHandler(
      PermissionEvaluator permissionEvaluator) {
    DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
    handler.setPermissionEvaluator(permissionEvaluator);
    return handler;
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      @Value("${polity.api.base-path}") String basePath,
      KeycloakAuthoritiesConverter authorities) {
    JwtAuthenticationConverter jwt = new JwtAuthenticationConverter();
    jwt.setJwtGrantedAuthoritiesConverter(authorities);
    return http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/openapi.json",
                        "/docs/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(basePath + "/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(configurer -> configurer.jwtAuthenticationConverter(jwt)))
        .build();
  }
}
