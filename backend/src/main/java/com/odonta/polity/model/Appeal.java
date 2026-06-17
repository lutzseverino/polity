package com.odonta.polity.model;

import com.odonta.common.data.AuditedEntity;
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
@Table(name = "appeals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appeal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "sanction_id", nullable = false)
  private UUID sanctionId;

  @Column(name = "appellant_membership_id", nullable = false)
  private UUID appellantMembershipId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AppealStatus status;

  @Column(nullable = false)
  private String reason;

  @Column(name = "decided_at", nullable = false)
  private OffsetDateTime decidedAt;

  public Appeal(
      UUID polityId,
      UUID motionId,
      UUID sanctionId,
      UUID appellantMembershipId,
      String reason,
      OffsetDateTime decidedAt) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.sanctionId = sanctionId;
    this.appellantMembershipId = appellantMembershipId;
    this.status = AppealStatus.GRANTED;
    this.reason = reason;
    this.decidedAt = decidedAt;
  }
}
