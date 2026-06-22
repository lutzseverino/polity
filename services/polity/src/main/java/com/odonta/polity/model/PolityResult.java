package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PolityResult(
    UUID id,
    String name,
    PolityVisibility visibility,
    PolityStatus status,
    int constitutionVersion,
    String jurisdictionName,
    String institutionName,
    OffsetDateTime createdAt) {}
