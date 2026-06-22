package com.odonta.polity.model;

public record ConstitutionPowerResult(
    PowerCode code,
    String name,
    String nameKey,
    PowerHolderScope holderScope,
    String holderOfficeCode) {}
