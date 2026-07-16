package com.odonta.polity.result;

import com.odonta.polity.model.ConstitutionChangeOperation;
import java.util.UUID;

public record ConstitutionOfficeChangeResult(
    ConstitutionChangeOperation action,
    String code,
    UUID jurisdictionId,
    String name,
    String description,
    Integer termLengthDays,
    Integer seatCount) {}
