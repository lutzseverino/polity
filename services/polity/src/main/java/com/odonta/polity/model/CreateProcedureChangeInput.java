package com.odonta.polity.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@ValidProcedureChange
public record CreateProcedureChangeInput(
    @NotBlank String procedureCode,
    @Min(1) Integer quorumNumerator,
    @Min(1) Integer quorumDenominator,
    VotingThreshold threshold,
    @Min(0) Integer minimumNoticeHours,
    @Min(1) Integer votingPeriodHours) {}
