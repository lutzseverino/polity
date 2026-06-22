package com.odonta.polity.model;

import java.util.UUID;

public record OfficeResult(
    UUID id,
    String code,
    String name,
    String description,
    String nameKey,
    String descriptionKey,
    int termLengthDays) {}
