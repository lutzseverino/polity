package com.odonta.polity.result;

public record MotionActionAvailabilityResult(
    ActionAvailabilityResult castVote,
    ActionAvailabilityResult castElectionBallot,
    ActionAvailabilityResult respondCandidacy,
    ActionAvailabilityResult requestCertification) {}
