package com.odonta.polity.result;

import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import java.util.UUID;

public record ConstitutionProcedureChangeResult(
    String procedureCode,
    UUID institutionId,
    Integer quorumNumerator,
    Integer quorumDenominator,
    VotingThreshold threshold,
    OfficeElectionMethod officeElectionMethod,
    ProcedureElectorate electorate,
    String electorateOfficeCode,
    Integer minimumElectorCount,
    Integer minimumNoticeHours,
    Integer votingPeriodHours) {}
