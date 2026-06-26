package com.odonta.polity;

public final class PolityPermissions {
  public static final String CLIENT_ID = "polity";
  public static final String READ = "read";
  public static final String PARTICIPATE = "participate";
  public static final String PUBLIC_POLITY_CREATE = "public-polity:create";
  public static final String PUBLIC_POLITY_CREATE_AUTHORITY =
      CLIENT_ID + ":" + PUBLIC_POLITY_CREATE;

  public static final String POLITY_RESOURCE = "polity:polity";

  public static final String HAS_PUBLIC_POLITY_CREATE =
      "hasAuthority('" + PUBLIC_POLITY_CREATE_AUTHORITY + "')";
  public static final String HAS_POLITY_READ =
      "hasPermission(#polityId, '" + POLITY_RESOURCE + "', '" + READ + "')";
  public static final String HAS_POLITY_PARTICIPATE =
      "hasPermission(#polityId, '" + POLITY_RESOURCE + "', '" + PARTICIPATE + "')";
  public static final String CAN_READ_POLITY =
      HAS_POLITY_READ + " or @polityAccessPolicy.isPublic(#polityId)";

  private PolityPermissions() {}
}
