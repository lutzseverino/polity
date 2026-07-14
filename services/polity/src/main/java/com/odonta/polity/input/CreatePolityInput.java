package com.odonta.polity.input;

import com.odonta.polity.model.PolityPace;
import com.odonta.polity.model.PolitySetupPreset;
import com.odonta.polity.model.PolityVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePolityInput(
    @NotBlank @Size(max = 120) String name,
    @NotNull PolityVisibility visibility,
    PolitySetupPreset setupPreset,
    PolityPace pace) {
  public PolitySetupPreset setupPresetOrDefault() {
    return setupPreset == null
        ? PolitySetupPreset.STANDARD_CONSTITUTIONAL_COUNCIL_REPUBLIC
        : setupPreset;
  }

  public PolityPace paceOrDefault() {
    return pace == null ? PolityPace.STANDARD : pace;
  }
}
