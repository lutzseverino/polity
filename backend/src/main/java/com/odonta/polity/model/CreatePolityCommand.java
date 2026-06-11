package com.odonta.polity.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePolityCommand(@NotBlank @Size(max = 120) String name) {}
