package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record MotionResult(
    UUID id,
    String title,
    String body,
    String titleKey,
    String bodyKey,
    Map<String, Object> templateParams,
    MotionStatus status,
    EffectType effectType,
    int constitutionVersion,
    String procedureName,
    String procedureNameKey,
    String introducedByName,
    OffsetDateTime openedAt,
    OffsetDateTime votingOpensAt,
    OffsetDateTime votingClosesAt,
    OffsetDateTime certificationOpensAt,
    VotingResult tally,
    OfficeElectionResult officeElection,
    OfficeElectionTallyResult electionTally,
    CertificationResult certification) {}
