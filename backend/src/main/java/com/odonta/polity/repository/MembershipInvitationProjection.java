package com.odonta.polity.repository;

import com.odonta.polity.model.InvitationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface MembershipInvitationProjection {
  UUID getId();

  UUID getPolityId();

  String getPolityName();

  String getEmail();

  String getInvitedByName();

  InvitationStatus getStatus();

  OffsetDateTime getInvitedAt();

  OffsetDateTime getRespondedAt();
}
