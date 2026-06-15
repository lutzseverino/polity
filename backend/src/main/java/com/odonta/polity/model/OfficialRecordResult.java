package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfficialRecordResult(
    UUID id,
    OfficialRecordType type,
    String title,
    String body,
    String actorName,
    int constitutionVersion,
    UUID sourceId,
    OffsetDateTime occurredAt) {}
