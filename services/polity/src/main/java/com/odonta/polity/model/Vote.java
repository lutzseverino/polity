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
@Table(name = "votes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "membership_id", nullable = false)
  private UUID membershipId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VoteChoice choice;

  @Column(name = "cast_at", nullable = false)
  private OffsetDateTime castAt;

  public Vote(
      UUID polityId, UUID motionId, UUID membershipId, VoteChoice choice, OffsetDateTime castAt) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.membershipId = membershipId;
    this.choice = choice;
    this.castAt = castAt;
  }

  public void replace(VoteChoice choice, OffsetDateTime castAt) {
    this.choice = choice;
    this.castAt = castAt;
  }
}
