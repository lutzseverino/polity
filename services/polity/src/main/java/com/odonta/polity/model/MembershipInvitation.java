package com.odonta.polity.model;

import io.github.lutzseverino.cardo.common.data.AuditedEntity;
import io.github.lutzseverino.cardo.common.data.PersonalDataEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "membership_invitations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipInvitation extends AuditedEntity implements PersonalDataEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "invited_user_id")
  private UUID invitedUserId;

  @Column(name = "cardo_invitation_id", unique = true)
  private UUID cardoInvitationId;

  @Column(name = "cardo_expires_at")
  private OffsetDateTime cardoExpiresAt;

  @Column(nullable = false)
  private String email;

  @Column(name = "invited_by", nullable = false)
  private UUID invitedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MembershipInvitationStatus status;

  @Column(name = "invited_at", nullable = false)
  private OffsetDateTime invitedAt;

  @Column(name = "responded_at")
  private OffsetDateTime respondedAt;

  public MembershipInvitation(
      UUID polityId, String email, UUID invitedBy, OffsetDateTime invitedAt) {
    this.polityId = polityId;
    this.email = email;
    this.invitedBy = invitedBy;
    this.status = MembershipInvitationStatus.PENDING;
    this.invitedAt = invitedAt;
  }

  public void accept(OffsetDateTime acceptedAt) {
    this.status = MembershipInvitationStatus.ACCEPTED;
    this.respondedAt = acceptedAt;
  }

  public void registerCardoInvitation(UUID invitationId, UUID userId, OffsetDateTime expiresAt) {
    Objects.requireNonNull(invitationId, "Cardo invitation ID is required.");
    Objects.requireNonNull(userId, "Invited user ID is required.");
    Objects.requireNonNull(expiresAt, "Cardo invitation expiry is required.");
    if (cardoInvitationId != null && !cardoInvitationId.equals(invitationId)) {
      throw new IllegalStateException(
          "Membership invitation is linked to another Cardo invitation.");
    }
    if (invitedUserId != null && !invitedUserId.equals(userId)) {
      throw new IllegalStateException("Membership invitation is linked to another invited user.");
    }
    if (cardoExpiresAt != null && !cardoExpiresAt.isEqual(expiresAt)) {
      throw new IllegalStateException("Membership invitation has another Cardo expiry.");
    }
    this.cardoInvitationId = invitationId;
    this.invitedUserId = userId;
    this.cardoExpiresAt = expiresAt;
  }
}
