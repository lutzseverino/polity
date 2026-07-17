package com.odonta.polity.repository;

import com.odonta.polity.model.MembershipInvitationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface MembershipInvitationProjection {
  UUID getId();

  UUID getPolityId();

  UUID getInvitedUserId();

  UUID getCardoInvitationId();

  OffsetDateTime getCardoExpiresAt();

  String getEmail();

  UUID getInvitedBy();

  MembershipInvitationStatus getStatus();

  OffsetDateTime getInvitedAt();

  OffsetDateTime getRespondedAt();
}
