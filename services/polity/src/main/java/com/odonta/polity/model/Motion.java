package com.odonta.polity.model;

import io.github.lutzseverino.cardo.common.data.AuditedEntity;
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
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  @Column(name = "title_key")
  private String titleKey;

  @Column(name = "body_key")
  private String bodyKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "template_params", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> templateParams = Map.of();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MotionStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "effect_type", nullable = false)
  private EffectType effectType;

  @Column(name = "opened_at", nullable = false)
  private OffsetDateTime openedAt;

  @Column(name = "voting_opens_at", nullable = false)
  private OffsetDateTime votingOpensAt;

  @Column(name = "voting_closes_at", nullable = false)
  private OffsetDateTime votingClosesAt;

  @Column(name = "certification_opens_at", nullable = false)
  private OffsetDateTime certificationOpensAt;

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
      OffsetDateTime openedAt,
      OffsetDateTime votingOpensAt,
      OffsetDateTime votingClosesAt,
      OffsetDateTime certificationOpensAt) {
    this(
        polityId,
        jurisdictionId,
        institutionId,
        constitutionVersionId,
        procedureId,
        introducedBy,
        title,
        body,
        null,
        effectType,
        openedAt,
        votingOpensAt,
        votingClosesAt,
        certificationOpensAt);
  }

  public Motion(
      UUID polityId,
      UUID jurisdictionId,
      UUID institutionId,
      UUID constitutionVersionId,
      UUID procedureId,
      UUID introducedBy,
      String title,
      String body,
      MotionTemplate template,
      EffectType effectType,
      OffsetDateTime openedAt,
      OffsetDateTime votingOpensAt,
      OffsetDateTime votingClosesAt,
      OffsetDateTime certificationOpensAt) {
    this.polityId = polityId;
    this.jurisdictionId = jurisdictionId;
    this.institutionId = institutionId;
    this.constitutionVersionId = constitutionVersionId;
    this.procedureId = procedureId;
    this.introducedBy = introducedBy;
    this.title = title;
    this.body = body;
    if (template != null) {
      this.titleKey = template.titleKey();
      this.bodyKey = template.bodyKey();
      this.templateParams = template.params();
    }
    this.effectType = effectType;
    this.status = MotionStatus.VOTING;
    this.openedAt = openedAt;
    this.votingOpensAt = votingOpensAt;
    this.votingClosesAt = votingClosesAt;
    this.certificationOpensAt = certificationOpensAt;
  }

  public void certify(boolean passed, OffsetDateTime certifiedAt) {
    if (status != MotionStatus.VOTING) {
      throw new IllegalStateException("Only voting motions can be certified");
    }
    this.status = passed ? MotionStatus.ENACTED : MotionStatus.REJECTED;
    this.certifiedAt = certifiedAt;
  }
}
