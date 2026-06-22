package com.odonta.polity.model;

import java.util.Objects;
import java.util.UUID;

public sealed interface OfficialRecordContext
    permits OfficialRecordContext.None,
        OfficialRecordContext.MotionContext,
        OfficialRecordContext.CertificationContext,
        OfficialRecordContext.EffectContext {

  default UUID motionId() {
    return null;
  }

  default UUID procedureId() {
    return null;
  }

  default UUID institutionId() {
    return null;
  }

  default PowerCode powerCode() {
    return null;
  }

  default UUID certificationId() {
    return null;
  }

  default EffectType effectType() {
    return null;
  }

  default OfficialRecordOutcome outcome() {
    return null;
  }

  static OfficialRecordContext none() {
    return new None();
  }

  static OfficialRecordContext motion(
      Motion motion, PowerCode powerCode, OfficialRecordOutcome outcome) {
    return new MotionContext(
        motion.getId(),
        motion.getProcedureId(),
        motion.getInstitutionId(),
        powerCode,
        motion.getEffectType(),
        outcome);
  }

  static OfficialRecordContext motion(Motion motion, OfficialRecordOutcome outcome) {
    return motion(motion, null, outcome);
  }

  static OfficialRecordContext certification(
      Motion motion, PowerCode powerCode, UUID certificationId, OfficialRecordOutcome outcome) {
    return new CertificationContext(
        motion.getId(),
        motion.getProcedureId(),
        motion.getInstitutionId(),
        powerCode,
        certificationId,
        motion.getEffectType(),
        outcome);
  }

  static OfficialRecordContext effect(Motion motion, OfficialRecordOutcome outcome) {
    return new EffectContext(
        motion.getId(),
        motion.getProcedureId(),
        motion.getInstitutionId(),
        motion.getEffectType(),
        outcome);
  }

  record None() implements OfficialRecordContext {}

  record MotionContext(
      UUID motionId,
      UUID procedureId,
      UUID institutionId,
      PowerCode powerCode,
      EffectType effectType,
      OfficialRecordOutcome outcome)
      implements OfficialRecordContext {
    public MotionContext {
      Objects.requireNonNull(motionId, "motionId");
      Objects.requireNonNull(procedureId, "procedureId");
      Objects.requireNonNull(institutionId, "institutionId");
      Objects.requireNonNull(effectType, "effectType");
      Objects.requireNonNull(outcome, "outcome");
    }
  }

  record CertificationContext(
      UUID motionId,
      UUID procedureId,
      UUID institutionId,
      PowerCode powerCode,
      UUID certificationId,
      EffectType effectType,
      OfficialRecordOutcome outcome)
      implements OfficialRecordContext {
    public CertificationContext {
      Objects.requireNonNull(motionId, "motionId");
      Objects.requireNonNull(procedureId, "procedureId");
      Objects.requireNonNull(institutionId, "institutionId");
      Objects.requireNonNull(powerCode, "powerCode");
      Objects.requireNonNull(certificationId, "certificationId");
      Objects.requireNonNull(effectType, "effectType");
      Objects.requireNonNull(outcome, "outcome");
    }
  }

  record EffectContext(
      UUID motionId,
      UUID procedureId,
      UUID institutionId,
      EffectType effectType,
      OfficialRecordOutcome outcome)
      implements OfficialRecordContext {
    public EffectContext {
      Objects.requireNonNull(motionId, "motionId");
      Objects.requireNonNull(procedureId, "procedureId");
      Objects.requireNonNull(institutionId, "institutionId");
      Objects.requireNonNull(effectType, "effectType");
      Objects.requireNonNull(outcome, "outcome");
    }
  }
}
