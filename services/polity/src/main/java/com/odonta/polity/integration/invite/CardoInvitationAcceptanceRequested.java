package com.odonta.polity.integration.invite;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CardoInvitationAcceptanceRequested(UUID invitationId, OffsetDateTime acceptedAt) {}
