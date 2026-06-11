package com.odonta.polity.model;

public record MotionDetails(
    Motion motion,
    Procedure procedure,
    ConstitutionVersion constitution,
    Membership introducer,
    VotingResult tally,
    Certification certification) {}
