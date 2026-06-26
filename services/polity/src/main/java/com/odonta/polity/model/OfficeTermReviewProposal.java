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
@Table(name = "office_term_review_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficeTermReviewProposal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "office_term_id", nullable = false)
  private UUID officeTermId;

  @Column(name = "petitioner_membership_id", nullable = false)
  private UUID petitionerMembershipId;

  @Column(nullable = false)
  private String reason;

  public OfficeTermReviewProposal(
      UUID polityId, UUID motionId, UUID officeTermId, UUID petitionerMembershipId, String reason) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.officeTermId = officeTermId;
    this.petitionerMembershipId = petitionerMembershipId;
    this.reason = reason;
  }
}
