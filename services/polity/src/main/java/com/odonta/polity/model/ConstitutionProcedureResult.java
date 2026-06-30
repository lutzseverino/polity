package com.odonta.polity.model;

import java.util.UUID;

public record ConstitutionProcedureResult(
    UUID id,
    UUID institutionId,
    String code,
    String name,
    String nameKey,
    int quorumNumerator,
    int quorumDenominator,
    VotingThreshold threshold,
    OfficeElectionMethod officeElectionMethod,
    ProcedureElectorate electorate,
    String electorateOfficeCode,
    int minimumElectorCount,
    int minimumNoticeHours,
    int votingPeriodHours,
    EffectType effectType) {}
