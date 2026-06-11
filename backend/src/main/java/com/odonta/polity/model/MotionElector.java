package com.odonta.polity.model;

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
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(name = "motion_electors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MotionElector {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(name = "membership_id", nullable = false)
  private UUID membershipId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  public MotionElector(UUID polityId, UUID motionId, UUID membershipId) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.membershipId = membershipId;
  }
}
