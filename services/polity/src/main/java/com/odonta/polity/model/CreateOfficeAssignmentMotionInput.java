package com.odonta.polity.model;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOfficeAssignmentMotionInput(
    @NotNull UUID officeId, @NotNull UUID nomineeMembershipId) {}
