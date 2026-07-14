package com.odonta.polity.input;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateMemberInvitationInput(@NotBlank @Email String email) {}
