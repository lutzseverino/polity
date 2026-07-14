package com.odonta.polity.input;

import com.odonta.polity.model.VoteChoice;
import jakarta.validation.constraints.NotNull;

public record CastVoteInput(@NotNull VoteChoice choice) {}
