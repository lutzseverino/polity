package com.odonta.polity.model;

import com.odonta.common.data.AuditedEntity;
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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CertificationModality modality;

  @Column(name = "eligible_count", nullable = false)
  private int eligibleCount;

  @Column(name = "yes_count")
  private Integer yesCount;

  @Column(name = "no_count")
  private Integer noCount;

  @Column(name = "abstain_count")
  private Integer abstainCount;

  @Column(name = "election_participation_count")
  private Integer electionParticipationCount;

  @Column(name = "election_decisive")
  private Boolean electionDecisive;

  @Column(name = "election_winner_membership_id")
  private UUID electionWinnerMembershipId;

  @Column(name = "election_winner_name")
  private String electionWinnerName;

  @Column(name = "quorum_required", nullable = false)
  private int quorumRequired;

  @Column(name = "quorum_met", nullable = false)
  private boolean quorumMet;

  @Column(name = "threshold_met", nullable = false)
  private boolean thresholdMet;

  @Column(nullable = false)
  private boolean passed;

  @Enumerated(EnumType.STRING)
  @Column(name = "outcome_reason", nullable = false)
  private CertificationOutcomeReason outcomeReason;

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
    this.modality = CertificationModality.YES_NO;
    this.eligibleCount = result.eligible();
    this.yesCount = result.yes();
    this.noCount = result.no();
    this.abstainCount = result.abstain();
    this.electionParticipationCount = null;
    this.electionDecisive = null;
    this.electionWinnerMembershipId = null;
    this.electionWinnerName = null;
    this.quorumRequired = result.quorumRequired();
    this.quorumMet = result.quorumMet();
    this.thresholdMet = result.thresholdMet();
    this.passed = result.passed();
    this.outcomeReason = outcomeReason(result);
    this.certifiedAt = certifiedAt;
  }

  public Certification(
      UUID polityId,
      UUID motionId,
      UUID requestedBy,
      OfficeElectionTallyResult result,
      OffsetDateTime certifiedAt) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.requestedBy = requestedBy;
    this.modality = CertificationModality.OFFICE_ELECTION;
    this.eligibleCount = result.eligible();
    this.yesCount = null;
    this.noCount = null;
    this.abstainCount = null;
    this.electionParticipationCount = result.participation();
    this.electionDecisive = result.decisive();
    this.electionWinnerMembershipId = result.winnerMembershipId();
    this.electionWinnerName = result.winnerName();
    this.quorumRequired = result.quorumRequired();
    this.quorumMet = result.quorumMet();
    this.thresholdMet = result.decisive();
    this.passed = result.passed();
    this.outcomeReason = outcomeReason(result);
    this.certifiedAt = certifiedAt;
  }

  private CertificationOutcomeReason outcomeReason(VotingResult result) {
    if (result.passed()) {
      return CertificationOutcomeReason.PASSED;
    }
    return result.quorumMet()
        ? CertificationOutcomeReason.THRESHOLD_NOT_MET
        : CertificationOutcomeReason.QUORUM_NOT_MET;
  }

  private CertificationOutcomeReason outcomeReason(OfficeElectionTallyResult result) {
    return switch (result.outcomeReason()) {
      case PASSED -> CertificationOutcomeReason.PASSED;
      case QUORUM_NOT_MET -> CertificationOutcomeReason.QUORUM_NOT_MET;
      case NO_DECISIVE_PLURALITY -> CertificationOutcomeReason.NO_DECISIVE_PLURALITY;
    };
  }
}
