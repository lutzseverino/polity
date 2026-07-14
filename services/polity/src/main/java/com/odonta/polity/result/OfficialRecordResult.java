package com.odonta.polity.result;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.TemplateParameters;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record OfficialRecordResult(
    UUID id,
    int entryNumber,
    OfficialRecordType type,
    String title,
    String body,
    String titleKey,
    String bodyKey,
    Map<String, Object> templateParams,
    String actorName,
    int constitutionVersion,
    UUID sourceId,
    UUID motionId,
    UUID procedureId,
    UUID institutionId,
    PowerCode powerCode,
    UUID certificationId,
    EffectType effectType,
    String outcome,
    OffsetDateTime occurredAt) {
  public OfficialRecordResult {
    templateParams = TemplateParameters.copyOf(templateParams);
  }
}
