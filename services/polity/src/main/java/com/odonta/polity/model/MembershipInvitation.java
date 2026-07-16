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

  @Column(name = "invited_user_id", nullable = false)
  private UUID invitedUserId;

  @Column(name = "authorization_subject", nullable = false)
  private String authorizationSubject;

  @Column(nullable = false)
  private String email;

  @Column(name = "invited_by", nullable = false)
  private UUID invitedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InvitationStatus status;

  @Column(name = "invited_at", nullable = false)
  private OffsetDateTime invitedAt;

  @Column(name = "responded_at")
  private OffsetDateTime respondedAt;

  public MembershipInvitation(
      UUID polityId,
      UUID invitedUserId,
      String authorizationSubject,
      String email,
      UUID invitedBy,
      OffsetDateTime invitedAt) {
    this.polityId = polityId;
    this.invitedUserId = invitedUserId;
    this.authorizationSubject = authorizationSubject;
    this.email = email;
    this.invitedBy = invitedBy;
    this.status = InvitationStatus.PENDING;
    this.invitedAt = invitedAt;
  }

  public void accept(OffsetDateTime acceptedAt) {
    this.status = InvitationStatus.ACCEPTED;
    this.respondedAt = acceptedAt;
  }
}
