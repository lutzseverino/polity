package com.odonta.polity.model;

import com.odonta.common.data.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "constitutional_review_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConstitutionalReviewProposal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "target_record_id", nullable = false)
  private UUID targetRecordId;

  @Column(name = "petitioner_membership_id", nullable = false)
  private UUID petitionerMembershipId;

  @Column(nullable = false)
  private String reason;

  public ConstitutionalReviewProposal(
      UUID polityId,
      UUID motionId,
      UUID targetRecordId,
      UUID petitionerMembershipId,
      String reason) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.targetRecordId = targetRecordId;
    this.petitionerMembershipId = petitionerMembershipId;
    this.reason = reason;
  }
}
