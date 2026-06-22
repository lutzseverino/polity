package com.odonta.polity.model;

import java.util.UUID;

public record ConstitutionProcedureResult(
    UUID id,
    String code,
    String name,
    String nameKey,
    int quorumNumerator,
    int quorumDenominator,
    VotingThreshold threshold,
    ProcedureElectorate electorate,
    String electorateOfficeCode,
    int minimumNoticeHours,
    int votingPeriodHours,
    EffectType effectType) {}
