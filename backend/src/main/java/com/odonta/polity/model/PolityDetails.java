package com.odonta.polity.model;

public record PolityDetails(
    Polity polity,
    ConstitutionVersion constitution,
    Jurisdiction jurisdiction,
    Institution institution) {}
