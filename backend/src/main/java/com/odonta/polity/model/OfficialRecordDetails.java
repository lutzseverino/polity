package com.odonta.polity.model;

public record OfficialRecordDetails(
    OfficialRecordEntry entry, Membership actor, ConstitutionVersion constitution) {}
