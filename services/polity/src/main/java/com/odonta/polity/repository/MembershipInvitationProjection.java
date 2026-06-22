package com.odonta.polity.repository;

import com.odonta.polity.model.InvitationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface MembershipInvitationProjection {
  UUID getId();

  UUID getPolityId();

  String getEmail();

  UUID getInvitedBy();

  InvitationStatus getStatus();

  OffsetDateTime getInvitedAt();

  OffsetDateTime getRespondedAt();
}
