package com.odonta.polity.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateSanctionMotionInput(
    @NotNull UUID targetMembershipId,
    @NotNull SanctionType type,
    @NotBlank @Size(max = 5000) String reason,
    @Positive int durationDays) {}
