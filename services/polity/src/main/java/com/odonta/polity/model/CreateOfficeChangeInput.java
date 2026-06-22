package com.odonta.polity.model;

import com.odonta.polity.validation.ValidOfficeChange;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ValidOfficeChange
public record CreateOfficeChangeInput(
    @NotNull ConstitutionOfficeChangeAction action,
    @NotBlank @Size(max = 64) @Pattern(regexp = "^[a-z][a-z0-9-]*$") String code,
    @Size(max = 120) String name,
    @Size(max = 1000) String description,
    @Min(1) Integer termLengthDays) {}
