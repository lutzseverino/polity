package com.odonta.polity.model;

import jakarta.validation.constraints.NotNull;

public record CastVoteInput(@NotNull VoteChoice choice) {}
