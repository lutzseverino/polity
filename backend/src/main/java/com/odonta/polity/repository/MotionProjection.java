package com.odonta.polity.repository;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.VotingProcedure;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface MotionProjection extends VotingProcedure {
  UUID getId();

  String getTitle();

  String getBody();

  MotionStatus getStatus();

  EffectType getEffectType();

  int getConstitutionVersion();

  String getProcedureName();

  String getIntroducedByName();

  OffsetDateTime getOpenedAt();
}
