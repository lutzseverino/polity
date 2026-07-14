package com.odonta.polity.result;

import java.util.UUID;

public record OfficeResult(
    UUID id,
    UUID jurisdictionId,
    String code,
    String name,
    String description,
    String nameKey,
    String descriptionKey,
    int termLengthDays,
    int seatCount) {}
