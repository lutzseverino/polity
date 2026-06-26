package com.odonta.polity.model;

import com.odonta.polity.validation.ValidProcedureChange;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@ValidProcedureChange
public record CreateProcedureChangeInput(
    @NotBlank String procedureCode,
    UUID institutionId,
    @Min(1) Integer quorumNumerator,
    @Min(1) Integer quorumDenominator,
    VotingThreshold threshold,
    ProcedureElectorate electorate,
    @Size(max = 64) @Pattern(regexp = "^[a-z][a-z0-9-]*$") String electorateOfficeCode,
    @Min(1) Integer minimumElectorCount,
    @Min(0) Integer minimumNoticeHours,
    @Min(1) Integer votingPeriodHours) {
  public CreateProcedureChangeInput(
      String procedureCode,
      Integer quorumNumerator,
      Integer quorumDenominator,
      VotingThreshold threshold,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      Integer minimumElectorCount,
      Integer minimumNoticeHours,
      Integer votingPeriodHours) {
    this(
        procedureCode,
        null,
        quorumNumerator,
        quorumDenominator,
        threshold,
        electorate,
        electorateOfficeCode,
        minimumElectorCount,
        minimumNoticeHours,
        votingPeriodHours);
  }
}
