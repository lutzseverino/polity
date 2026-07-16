package com.odonta.polity.model;

import io.github.lutzseverino.cardo.common.data.AuditedEntity;
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
@Table(name = "office_terms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficeTerm extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "office_id", nullable = false)
  private UUID officeId;

  @Column(name = "office_code", nullable = false)
  private String officeCode;

  @Column(name = "membership_id", nullable = false)
  private UUID membershipId;

  @Column(name = "assigned_by_motion_id")
  private UUID assignedByMotionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OfficeTermStatus status;

  @Column(name = "started_at", nullable = false)
  private OffsetDateTime startedAt;

  @Column(name = "ends_at", nullable = false)
  private OffsetDateTime endsAt;

  @Column(name = "ended_at")
  private OffsetDateTime endedAt;

  public OfficeTerm(
      UUID polityId,
      UUID officeId,
      String officeCode,
      UUID membershipId,
      UUID assignedByMotionId,
      OffsetDateTime startedAt,
      OffsetDateTime endsAt) {
    this.polityId = polityId;
    this.officeId = officeId;
    this.officeCode = officeCode;
    this.membershipId = membershipId;
    this.assignedByMotionId = assignedByMotionId;
    this.status = OfficeTermStatus.ACTIVE;
    this.startedAt = startedAt;
    this.endsAt = endsAt;
  }

  public OfficeTerm(
      UUID polityId,
      UUID officeId,
      String officeCode,
      UUID membershipId,
      OffsetDateTime startedAt,
      OffsetDateTime endsAt) {
    this.polityId = polityId;
    this.officeId = officeId;
    this.officeCode = officeCode;
    this.membershipId = membershipId;
    this.status = OfficeTermStatus.ACTIVE;
    this.startedAt = startedAt;
    this.endsAt = endsAt;
  }

  public void end(OffsetDateTime endedAt) {
    this.status = OfficeTermStatus.ENDED;
    this.endedAt = endedAt;
  }

  public boolean isActiveAt(OffsetDateTime when) {
    return status == OfficeTermStatus.ACTIVE && endsAt.isAfter(when);
  }
}
