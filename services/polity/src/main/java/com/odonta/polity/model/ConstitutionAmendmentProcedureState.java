package com.odonta.polity.model;

import java.util.UUID;

public record ConstitutionAmendmentProcedureState(
    UUID institutionId,
    EffectType effectType,
    VotingThreshold threshold,
    OfficeElectionMethod officeElectionMethod,
    ProcedureElectorate electorate,
    String electorateOfficeCode) {}
