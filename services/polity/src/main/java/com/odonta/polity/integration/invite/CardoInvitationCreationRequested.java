package com.odonta.polity.integration.invite;

import java.util.UUID;

public record CardoInvitationCreationRequested(UUID invitationId, UUID invitedByUserId) {}
