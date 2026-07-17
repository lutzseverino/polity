package com.odonta.polity.result;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipInvitationTokenResult(
    UUID polityId, String polityName, String invitedEmail, OffsetDateTime expiresAt) {}
