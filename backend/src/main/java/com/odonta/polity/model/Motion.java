package com.odonta.polity.model;

import com.odonta.common.data.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "motions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Motion extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "jurisdiction_id", nullable = false)
  private UUID jurisdictionId;

  @Column(name = "institution_id", nullable = false)
  private UUID institutionId;

  @Column(name = "constitution_version_id", nullable = false)
  private UUID constitutionVersionId;

  @Column(name = "procedure_id", nullable = false)
  private UUID procedureId;

  @Column(name = "introduced_by", nullable = false)
  private UUID introducedBy;

  @NotBlank @Size(max = 200) @Column(nullable = false)
  private String title;

  @NotBlank @Size(max = 5000) @Column(nullable = false)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MotionStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "effect_type", nullable = false)
  private EffectType effectType;

  @Column(name = "opened_at", nullable = false)
  private OffsetDateTime openedAt;

  @Column(name = "certified_at")
  private OffsetDateTime certifiedAt;

  public Motion(
      UUID polityId,
      UUID jurisdictionId,
      UUID institutionId,
      UUID constitutionVersionId,
      UUID procedureId,
      UUID introducedBy,
      String title,
      String body,
      EffectType effectType,
      OffsetDateTime openedAt) {
    this.polityId = polityId;
    this.jurisdictionId = jurisdictionId;
    this.institutionId = institutionId;
    this.constitutionVersionId = constitutionVersionId;
    this.procedureId = procedureId;
    this.introducedBy = introducedBy;
    this.title = title;
    this.body = body;
    this.effectType = effectType;
    this.status = MotionStatus.VOTING;
    this.openedAt = openedAt;
  }

  public void certify(boolean passed, OffsetDateTime certifiedAt) {
    if (status != MotionStatus.VOTING) {
      throw new IllegalStateException("Only voting motions can be certified");
    }
    this.status = passed ? MotionStatus.ENACTED : MotionStatus.REJECTED;
    this.certifiedAt = certifiedAt;
  }
}
