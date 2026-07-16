package com.odonta.polity;

import io.github.lutzseverino.cardo.authorization.resource.AuthorizationResourceType;
import java.util.List;

public final class PolityResources {
  public static final String PRODUCT = PolityPermissions.CLIENT_ID;

  public static final AuthorizationResourceType POLITY =
      AuthorizationResourceType.of(
          PolityPermissions.CLIENT_ID,
          "polity",
          List.of(PolityPermissions.READ, PolityPermissions.PARTICIPATE));

  private PolityResources() {}
}
