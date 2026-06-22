package com.odonta.polity.model;

import com.odonta.polity.validation.ValidProcedureChange;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ValidProcedureChange
public record CreateProcedureChangeInput(
    @NotBlank String procedureCode,
    @Min(1) Integer quorumNumerator,
    @Min(1) Integer quorumDenominator,
    VotingThreshold threshold,
    ProcedureElectorate electorate,
    @Size(max = 64) @Pattern(regexp = "^[a-z][a-z0-9-]*$") String electorateOfficeCode,
    @Min(0) Integer minimumNoticeHours,
    @Min(1) Integer votingPeriodHours) {}
