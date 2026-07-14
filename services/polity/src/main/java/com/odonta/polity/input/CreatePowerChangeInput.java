package com.odonta.polity.input;

import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.validation.ValidPowerChange;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ValidPowerChange
public record CreatePowerChangeInput(
    @NotNull PowerCode powerCode,
    @NotNull PowerHolderScope holderScope,
    @Size(max = 64) @Pattern(regexp = "^[a-z][a-z0-9-]*$") String holderOfficeCode) {}
