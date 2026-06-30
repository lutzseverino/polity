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
@Table(name = "office_election_ballot_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficeElectionBallotPreference extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "ballot_id", nullable = false)
  private UUID ballotId;

  @Column(name = "membership_id", nullable = false)
  private UUID membershipId;

  @Column(name = "candidate_membership_id", nullable = false)
  private UUID candidateMembershipId;

  @Column(name = "rank", nullable = false)
  private int rank;

  public OfficeElectionBallotPreference(
      UUID polityId,
      UUID motionId,
      UUID ballotId,
      UUID membershipId,
      UUID candidateMembershipId,
      int rank) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.ballotId = ballotId;
    this.membershipId = membershipId;
    this.candidateMembershipId = candidateMembershipId;
    this.rank = rank;
  }
}
