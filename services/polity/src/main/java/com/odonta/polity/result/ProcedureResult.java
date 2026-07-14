package com.odonta.polity.result;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import java.util.UUID;

public record ProcedureResult(
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
