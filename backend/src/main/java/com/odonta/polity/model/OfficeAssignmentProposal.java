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
@Table(name = "office_assignment_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficeAssignmentProposal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "office_id", nullable = false)
  private UUID officeId;

  @Column(name = "nominee_membership_id", nullable = false)
  private UUID nomineeMembershipId;

  public OfficeAssignmentProposal(
      UUID polityId, UUID motionId, UUID officeId, UUID nomineeMembershipId) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.officeId = officeId;
    this.nomineeMembershipId = nomineeMembershipId;
  }
}
