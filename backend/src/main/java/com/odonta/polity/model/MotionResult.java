package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MotionResult(
    UUID id,
    String title,
    String body,
    MotionStatus status,
    EffectType effectType,
    int constitutionVersion,
    String procedureName,
    String introducedByName,
    OffsetDateTime openedAt,
    OffsetDateTime votingOpensAt,
    OffsetDateTime votingClosesAt,
    OffsetDateTime certificationOpensAt,
    VotingResult tally,
    CertificationResult certification) {}
