package com.odonta.polity.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMotionInput(
    @NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 5000) String body) {}
