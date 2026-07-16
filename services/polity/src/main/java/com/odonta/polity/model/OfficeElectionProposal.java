package com.odonta.polity.model;

import io.github.lutzseverino.cardo.common.data.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "office_election_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficeElectionProposal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "office_id", nullable = false)
  private UUID officeId;

  @Column(name = "seats_available", nullable = false)
  private int seatsAvailable;

  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false)
  private OfficeElectionMethod method;

  public OfficeElectionProposal(
      UUID polityId,
      UUID motionId,
      UUID officeId,
      int seatsAvailable,
      OfficeElectionMethod method) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.officeId = officeId;
    this.seatsAvailable = seatsAvailable;
    this.method = method;
  }
}
