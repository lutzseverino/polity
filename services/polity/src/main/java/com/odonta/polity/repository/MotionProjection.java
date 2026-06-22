package com.odonta.polity.repository;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.MotionStatus;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public interface MotionProjection {
  UUID getId();

  UUID getConstitutionVersionId();

  UUID getProcedureId();

  UUID getIntroducedBy();

  String getTitle();

  String getBody();

  String getTitleKey();

  String getBodyKey();

  Map<String, Object> getTemplateParams();

  MotionStatus getStatus();

  EffectType getEffectType();

  OffsetDateTime getOpenedAt();

  OffsetDateTime getVotingOpensAt();

  OffsetDateTime getVotingClosesAt();

  OffsetDateTime getCertificationOpensAt();
}
