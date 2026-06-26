package com.odonta.polity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "official_record_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialRecordEntry {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "entry_number", nullable = false)
  private int entryNumber;

  @Column(name = "jurisdiction_id", nullable = false)
  private UUID jurisdictionId;

  @Column(name = "constitution_version_id", nullable = false)
  private UUID constitutionVersionId;

  @Column(name = "actor_membership_id", nullable = false)
  private UUID actorMembershipId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OfficialRecordType type;

  @Column(name = "source_id")
  private UUID sourceId;

  @Column(name = "motion_id")
  private UUID motionId;

  @Column(name = "procedure_id")
  private UUID procedureId;

  @Column(name = "institution_id")
  private UUID institutionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "power_code")
  private PowerCode powerCode;

  @Column(name = "certification_id")
  private UUID certificationId;

  @Enumerated(EnumType.STRING)
  @Column(name = "effect_type")
  private EffectType effectType;

  @Column(name = "outcome")
  private String outcome;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String body;

  @Column(name = "title_key")
  private String titleKey;

  @Column(name = "body_key")
  private String bodyKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "template_params", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> templateParams = Map.of();

  @Column(name = "occurred_at", nullable = false)
  private OffsetDateTime occurredAt;

  public OfficialRecordEntry(
      UUID polityId,
      int entryNumber,
      UUID jurisdictionId,
      UUID constitutionVersionId,
      UUID actorMembershipId,
      OfficialRecordType type,
      UUID sourceId,
      OfficialRecordContext context,
      OfficialRecordTemplate template,
      OffsetDateTime occurredAt) {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(template, "template");
    this.polityId = polityId;
    this.entryNumber = entryNumber;
    this.jurisdictionId = jurisdictionId;
    this.constitutionVersionId = constitutionVersionId;
    this.actorMembershipId = actorMembershipId;
    this.type = type;
    this.sourceId = sourceId;
    this.motionId = context.motionId();
    this.procedureId = context.procedureId();
    this.institutionId = context.institutionId();
    this.powerCode = context.powerCode();
    this.certificationId = context.certificationId();
    this.effectType = context.effectType();
    this.outcome = context.outcome() == null ? null : context.outcome().value();
    this.title = template.storedTitle();
    this.body = template.storedBody();
    this.titleKey = template.titleKey();
    this.bodyKey = template.bodyKey();
    this.templateParams = template.params();
    this.occurredAt = occurredAt;
  }
}
