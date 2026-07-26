package com.odonta.polity.config;

import io.github.lutzseverino.cardo.identity.productauth.ProductRequestPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Polity's product route policy.
 *
 * <p>Cardo's {@code identity-product-auth} owns the filter chain, CSRF selection, browser-session
 * validation, server-side product-token acquisition, and authority construction. It permits {@code
 * /actuator/health}, {@code /actuator/info}, and the API documentation routes itself, applies this
 * policy, and then denies every request no rule matched.
 *
 * <p>Rules are method-aware so an unsupported method on a supported path falls through to that
 * denial rather than reaching a controller.
 */
@Configuration
public class SecurityConfig {
  @Bean
  ProductRequestPolicy productRequestPolicy(@Value("${polity.api.base-path}") String basePath) {
    return rules ->
        rules
            // Container probes read the liveness and readiness groups beneath `/actuator/health`,
            // which Cardo's own permit does not cover.
            .permitAll("/actuator/health/**")
            // Invitation-token onboarding completes before a session exists.
            .permitAll(HttpMethod.GET, basePath + "/invitation-tokens/*")
            .permitAll(HttpMethod.GET, basePath + "/invitation-tokens/*/completion")
            .permitAll(HttpMethod.POST, basePath + "/invitation-tokens/*/completion")
            // Convergence surfaces authenticate the principal but must never require the product
            // grant they exist to converge. They are listed ahead of the general product rules so
            // tightening those cannot silently lock a user out of their own recovery path.
            .authenticated(HttpMethod.GET, basePath + "/polity/account")
            .authenticated(HttpMethod.POST, basePath + "/polity/account")
            .authenticated(HttpMethod.GET, basePath + "/polities/*/members/me/access")
            .authenticated(HttpMethod.POST, basePath + "/polities/*/members/me/access")
            // The Polity API supports these methods and no others.
            .authenticated(HttpMethod.GET, basePath + "/**")
            .authenticated(HttpMethod.POST, basePath + "/**")
            .authenticated(HttpMethod.PUT, basePath + "/**");
  }
}
