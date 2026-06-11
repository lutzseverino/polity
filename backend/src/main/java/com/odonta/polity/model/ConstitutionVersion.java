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
@Table(name = "constitution_versions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConstitutionVersion extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(nullable = false)
  private int version;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ConstitutionStatus status;

  @Column(name = "ratified_at", nullable = false)
  private OffsetDateTime ratifiedAt;

  public ConstitutionVersion(
      UUID polityId, int version, String title, String body, OffsetDateTime ratifiedAt) {
    this.polityId = polityId;
    this.version = version;
    this.title = title;
    this.body = body;
    this.status = ConstitutionStatus.RATIFIED;
    this.ratifiedAt = ratifiedAt;
  }
}
