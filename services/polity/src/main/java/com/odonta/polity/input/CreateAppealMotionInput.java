package com.odonta.polity.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateAppealMotionInput(
    @NotNull UUID sanctionId, @NotBlank @Size(max = 5000) String reason) {}
