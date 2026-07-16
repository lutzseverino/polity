package com.odonta.polity.model;

import io.github.lutzseverino.cardo.common.data.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "appeal_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppealProposal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "sanction_id", nullable = false)
  private UUID sanctionId;

  @Column(name = "appellant_membership_id", nullable = false)
  private UUID appellantMembershipId;

  @NotBlank @Column(nullable = false)
  private String reason;

  public AppealProposal(
      UUID polityId, UUID motionId, UUID sanctionId, UUID appellantMembershipId, String reason) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.sanctionId = sanctionId;
    this.appellantMembershipId = appellantMembershipId;
    this.reason = reason;
  }
}
