package com.odonta.polity.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePolityInput(
    @NotBlank @Size(max = 120) String name, @NotNull PolityVisibility visibility) {}
