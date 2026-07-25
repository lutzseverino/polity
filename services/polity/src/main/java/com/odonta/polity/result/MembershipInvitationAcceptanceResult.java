package com.odonta.polity.result;

import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationAcceptanceStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipInvitationAcceptanceResult(
    UUID invitationId,
    MembershipInvitationAcceptanceStatus status,
    OffsetDateTime requestedAt,
    OffsetDateTime completedAt,
    String failureCode) {

  public static MembershipInvitationAcceptanceResult from(MembershipInvitation invitation) {
    return new MembershipInvitationAcceptanceResult(
        invitation.getId(),
        invitation.getAcceptanceStatus(),
        invitation.getAcceptanceRequestedAt(),
        invitation.getAcceptanceCompletedAt(),
        invitation.getAcceptanceFailureCode());
  }
}
