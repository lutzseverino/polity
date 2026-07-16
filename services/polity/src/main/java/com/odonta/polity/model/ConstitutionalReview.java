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
@Table(name = "constitutional_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConstitutionalReview extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "target_record_id", nullable = false)
  private UUID targetRecordId;

  @Column(name = "petitioner_membership_id", nullable = false)
  private UUID petitionerMembershipId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ConstitutionalReviewStatus status;

  @Column(nullable = false)
  private String reason;

  @Column(name = "decided_at", nullable = false)
  private OffsetDateTime decidedAt;

  public ConstitutionalReview(
      UUID polityId,
      UUID motionId,
      UUID targetRecordId,
      UUID petitionerMembershipId,
      String reason,
      OffsetDateTime decidedAt) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.targetRecordId = targetRecordId;
    this.petitionerMembershipId = petitionerMembershipId;
    this.status = ConstitutionalReviewStatus.GRANTED;
    this.reason = reason;
    this.decidedAt = decidedAt;
  }
}
