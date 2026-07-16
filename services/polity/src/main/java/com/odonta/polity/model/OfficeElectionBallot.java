package com.odonta.polity.model;

import io.github.lutzseverino.cardo.common.data.AuditedEntity;
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
@Table(name = "office_election_ballots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficeElectionBallot extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "membership_id", nullable = false)
  private UUID membershipId;

  @Column(name = "cast_at", nullable = false)
  private OffsetDateTime castAt;

  public OfficeElectionBallot(
      UUID polityId, UUID motionId, UUID membershipId, OffsetDateTime castAt) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.membershipId = membershipId;
    this.castAt = castAt;
  }

  public void replace(OffsetDateTime castAt) {
    this.castAt = castAt;
  }
}
