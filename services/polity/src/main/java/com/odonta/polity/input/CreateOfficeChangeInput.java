package com.odonta.polity.input;

import com.odonta.polity.model.ConstitutionChangeOperation;
import com.odonta.polity.validation.ValidOfficeChange;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@ValidOfficeChange
public record CreateOfficeChangeInput(
    @NotNull ConstitutionChangeOperation action,
    @NotBlank @Size(max = 64) @Pattern(regexp = "^[a-z][a-z0-9-]*$") String code,
    UUID jurisdictionId,
    @Size(max = 120) String name,
    @Size(max = 1000) String description,
    @Min(1) Integer termLengthDays,
    @Min(1) Integer seatCount) {
  public CreateOfficeChangeInput(
      ConstitutionChangeOperation action,
      String code,
      String name,
      String description,
      Integer termLengthDays,
      Integer seatCount) {
    this(action, code, null, name, description, termLengthDays, seatCount);
  }
}
