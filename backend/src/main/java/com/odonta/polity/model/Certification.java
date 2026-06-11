package com.odonta.polity.model;

import com.odonta.common.data.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "certifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Certification extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "requested_by", nullable = false)
  private UUID requestedBy;

  @Column(name = "eligible_count", nullable = false)
  private int eligibleCount;

  @Column(name = "yes_count", nullable = false)
  private int yesCount;

  @Column(name = "no_count", nullable = false)
  private int noCount;

  @Column(name = "abstain_count", nullable = false)
  private int abstainCount;

  @Column(name = "quorum_required", nullable = false)
  private int quorumRequired;

  @Column(name = "quorum_met", nullable = false)
  private boolean quorumMet;

  @Column(name = "threshold_met", nullable = false)
  private boolean thresholdMet;

  @Column(nullable = false)
  private boolean passed;

  @Column(nullable = false)
  private String explanation;

  @Column(name = "certified_at", nullable = false)
  private OffsetDateTime certifiedAt;

  public Certification(
      UUID polityId,
      UUID motionId,
      UUID requestedBy,
      VotingResult result,
      OffsetDateTime certifiedAt) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.requestedBy = requestedBy;
    this.eligibleCount = result.eligible();
    this.yesCount = result.yes();
    this.noCount = result.no();
    this.abstainCount = result.abstain();
    this.quorumRequired = result.quorumRequired();
    this.quorumMet = result.quorumMet();
    this.thresholdMet = result.thresholdMet();
    this.passed = result.passed();
    this.explanation = result.explanation();
    this.certifiedAt = certifiedAt;
  }
}
