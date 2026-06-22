package com.odonta.polity.repository;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public interface OfficialRecordProjection {
  UUID getId();

  int getEntryNumber();

  UUID getConstitutionVersionId();

  UUID getActorMembershipId();

  OfficialRecordType getType();

  String getTitle();

  String getBody();

  String getTitleKey();

  String getBodyKey();

  Map<String, Object> getTemplateParams();

  UUID getSourceId();

  UUID getMotionId();

  UUID getProcedureId();

  UUID getInstitutionId();

  PowerCode getPowerCode();

  UUID getCertificationId();

  EffectType getEffectType();

  String getOutcome();

  OffsetDateTime getOccurredAt();
}
