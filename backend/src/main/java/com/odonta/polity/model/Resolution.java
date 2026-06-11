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
@Table(name = "resolutions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resolution extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "motion_id", nullable = false)
  private UUID motionId;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String body;

  @Column(name = "adopted_at", nullable = false)
  private OffsetDateTime adoptedAt;

  public Resolution(
      UUID polityId, UUID motionId, String title, String body, OffsetDateTime adoptedAt) {
    this.polityId = polityId;
    this.motionId = motionId;
    this.title = title;
    this.body = body;
    this.adoptedAt = adoptedAt;
  }
}
