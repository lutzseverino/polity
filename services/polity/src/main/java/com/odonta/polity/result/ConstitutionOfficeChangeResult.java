package com.odonta.polity.result;

import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import java.util.UUID;

public record ConstitutionOfficeChangeResult(
    ConstitutionOfficeChangeAction action,
    String code,
    UUID jurisdictionId,
    String name,
    String description,
    Integer termLengthDays,
    Integer seatCount) {}
