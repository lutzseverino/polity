package com.odonta.polity.result;

import java.util.UUID;

public record PolityAccountResult(UUID userId, GrantConvergenceResult grants) {}
