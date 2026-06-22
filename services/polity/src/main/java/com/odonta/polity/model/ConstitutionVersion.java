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
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  @Column(name = "title_key")
  private String titleKey;

  @Column(name = "body_key")
  private String bodyKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "template_params", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> templateParams = Map.of();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ConstitutionStatus status;

  @Column(name = "ratified_at", nullable = false)
  private OffsetDateTime ratifiedAt;

  public ConstitutionVersion(
      UUID polityId, int version, String title, String body, OffsetDateTime ratifiedAt) {
    this(polityId, version, title, body, null, ratifiedAt);
  }

  public ConstitutionVersion(
      UUID polityId,
      int version,
      String title,
      String body,
      ConstitutionTemplateKey templateKey,
      OffsetDateTime ratifiedAt) {
    this.polityId = polityId;
    this.version = version;
    this.title = title;
    this.body = body;
    if (templateKey != null) {
      this.titleKey = templateKey.titleKey();
      this.bodyKey = templateKey.bodyKey();
      this.templateParams = Map.of();
    }
    this.status = ConstitutionStatus.RATIFIED;
    this.ratifiedAt = ratifiedAt;
  }

  public void supersede() {
    if (status != ConstitutionStatus.RATIFIED) {
      throw new IllegalStateException("Only ratified constitutions can be superseded");
    }
    this.status = ConstitutionStatus.SUPERSEDED;
  }
}
