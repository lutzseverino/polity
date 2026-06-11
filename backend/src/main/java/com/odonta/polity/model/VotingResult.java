package com.odonta.polity.model;

public record VotingResult(
    int eligible,
    int yes,
    int no,
    int abstain,
    int quorumRequired,
    boolean quorumMet,
    boolean thresholdMet,
    boolean passed,
    String explanation) {}
