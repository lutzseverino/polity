package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfficialRecordResult(
    UUID id,
    int entryNumber,
    OfficialRecordType type,
    String title,
    String body,
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
    OffsetDateTime occurredAt) {}
