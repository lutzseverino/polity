package com.odonta.polity.model;

import com.odonta.common.data.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "sanction_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SanctionProposal extends AuditedEntity {
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

  @NotBlank @Column(nullable = false)
  private String reason;

  @Positive @Column(name = "duration_days", nullable = false)
  private int durationDays;

  public SanctionProposal(
      UUID polityId,
      UUID motionId,
      UUID targetMembershipId,
      SanctionType type,
      String reason,
      int durationDays) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.targetMembershipId = targetMembershipId;
    this.type = type;
    this.reason = reason;
    this.durationDays = durationDays;
  }
}
