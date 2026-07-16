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
@Table(name = "office_term_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficeTermReview extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "office_term_id", nullable = false)
  private UUID officeTermId;

  @Column(name = "petitioner_membership_id", nullable = false)
  private UUID petitionerMembershipId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OfficeTermReviewStatus status;

  @Column(nullable = false)
  private String reason;

  @Column(name = "decided_at", nullable = false)
  private OffsetDateTime decidedAt;

  public OfficeTermReview(
      UUID polityId,
      UUID motionId,
      UUID officeTermId,
      UUID petitionerMembershipId,
      String reason,
      OffsetDateTime decidedAt) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.officeTermId = officeTermId;
    this.petitionerMembershipId = petitionerMembershipId;
    this.status = OfficeTermReviewStatus.GRANTED;
    this.reason = reason;
    this.decidedAt = decidedAt;
  }
}
