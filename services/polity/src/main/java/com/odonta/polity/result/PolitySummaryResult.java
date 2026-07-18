package com.odonta.polity.result;

import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PolitySummaryResult(
    UUID id,
    String slug,
    String name,
    PolityVisibility visibility,
    PolityStatus status,
    int constitutionVersion,
    String jurisdictionName,
    String institutionName,
    String institutionNameKey,
    OffsetDateTime createdAt) {}
