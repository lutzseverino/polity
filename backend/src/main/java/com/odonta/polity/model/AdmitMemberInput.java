package com.odonta.polity.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdmitMemberInput(@NotBlank @Email String email) {}
