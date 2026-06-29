package com.odonta.polity.model;

import com.odonta.common.data.AuditedEntity;
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
@Table(name = "constitution_amendment_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConstitutionAmendmentProposal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @NotBlank @Column(nullable = false)
  private String title;

  @NotBlank @Column(nullable = false)
  private String body;

  public ConstitutionAmendmentProposal(UUID polityId, UUID motionId, String title, String body) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.title = title;
    this.body = body;
  }
}
