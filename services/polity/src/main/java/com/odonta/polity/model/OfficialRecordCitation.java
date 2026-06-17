package com.odonta.polity.model;

import java.util.UUID;

public record OfficialRecordCitation(
    UUID motionId,
    UUID procedureId,
    UUID institutionId,
    PowerCode powerCode,
    UUID certificationId,
    EffectType effectType,
    String outcome) {
  public static OfficialRecordCitation empty() {
    return new OfficialRecordCitation(null, null, null, null, null, null, null);
  }
}
