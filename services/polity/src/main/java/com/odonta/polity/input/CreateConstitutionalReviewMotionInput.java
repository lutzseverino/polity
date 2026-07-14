package com.odonta.polity.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateConstitutionalReviewMotionInput(
    @NotNull UUID targetRecordId, @NotBlank @Size(max = 5000) String reason) {}
