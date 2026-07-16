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
@Table(name = "office_election_candidates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficeElectionCandidate extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "membership_id", nullable = false)
  private UUID membershipId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OfficeElectionCandidateStatus status;

  @Column(name = "responded_at")
  private OffsetDateTime respondedAt;

  public OfficeElectionCandidate(UUID polityId, UUID motionId, UUID membershipId) {
    this(polityId, motionId, membershipId, OfficeElectionCandidateStatus.ACCEPTED, null);
  }

  public OfficeElectionCandidate(
      UUID polityId,
      UUID motionId,
      UUID membershipId,
      OfficeElectionCandidateStatus status,
      OffsetDateTime respondedAt) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.membershipId = membershipId;
    this.status = status;
    this.respondedAt = respondedAt;
  }

  public void respond(boolean accepted, OffsetDateTime respondedAt) {
    this.status =
        accepted ? OfficeElectionCandidateStatus.ACCEPTED : OfficeElectionCandidateStatus.DECLINED;
    this.respondedAt = respondedAt;
  }

  public void disqualify(OffsetDateTime disqualifiedAt) {
    this.status = OfficeElectionCandidateStatus.DISQUALIFIED;
    this.respondedAt = disqualifiedAt;
  }
}
