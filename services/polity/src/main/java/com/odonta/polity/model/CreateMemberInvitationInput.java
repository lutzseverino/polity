package com.odonta.polity.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateMemberInvitationInput(@NotBlank @Email String email) {}
