package com.odonta.polity.config;

import java.net.URI;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "polity.membership-invitations")
public record MembershipInvitationProperties(URI acceptUrlBase) {
  public MembershipInvitationProperties {
    Objects.requireNonNull(acceptUrlBase, "Membership invitation accept URL base is required.");
    if (!("http".equalsIgnoreCase(acceptUrlBase.getScheme())
            || "https".equalsIgnoreCase(acceptUrlBase.getScheme()))
        || acceptUrlBase.getHost() == null
        || acceptUrlBase.getHost().isBlank()
        || acceptUrlBase.getQuery() != null
        || acceptUrlBase.getFragment() != null) {
      throw new IllegalArgumentException(
          "Membership invitation accept URL base must be an absolute HTTP(S) URL without query or fragment.");
    }
  }
}
