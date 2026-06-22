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
@Table(name = "sanctions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sanction extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "target_membership_id", nullable = false)
  private UUID targetMembershipId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SanctionType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SanctionStatus status;

  @Column(nullable = false)
  private String reason;

  @Column(name = "started_at", nullable = false)
  private OffsetDateTime startedAt;

  @Column(name = "ends_at", nullable = false)
  private OffsetDateTime endsAt;

  @Column(name = "vacated_at")
  private OffsetDateTime vacatedAt;

  public Sanction(
      UUID polityId,
      UUID motionId,
      UUID targetMembershipId,
      SanctionType type,
      String reason,
      OffsetDateTime startedAt,
      OffsetDateTime endsAt) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.targetMembershipId = targetMembershipId;
    this.type = type;
    this.status = SanctionStatus.ACTIVE;
    this.reason = reason;
    this.startedAt = startedAt;
    this.endsAt = endsAt;
  }

  public void vacate(OffsetDateTime vacatedAt) {
    if (isInactiveAt(vacatedAt)) {
      throw new IllegalStateException("Only active sanctions can be vacated");
    }
    this.status = SanctionStatus.VACATED;
    this.vacatedAt = vacatedAt;
  }

  public boolean isActiveAt(OffsetDateTime now) {
    return status == SanctionStatus.ACTIVE && endsAt.isAfter(now);
  }

  public boolean isInactiveAt(OffsetDateTime now) {
    return status != SanctionStatus.ACTIVE || !endsAt.isAfter(now);
  }

  public SanctionStatus statusAt(OffsetDateTime now) {
    if (isActiveAt(now)) {
      return SanctionStatus.ACTIVE;
    }
    if (status == SanctionStatus.ACTIVE) {
      return SanctionStatus.EXPIRED;
    }
    return status;
  }
}
