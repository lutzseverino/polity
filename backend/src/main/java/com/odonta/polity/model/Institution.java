package com.odonta.polity.model;

import com.odonta.common.data.AuditedEntity;
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
@Table(name = "institutions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Institution extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "jurisdiction_id", nullable = false)
  private UUID jurisdictionId;

  @Column(name = "constitution_version_id", nullable = false)
  private UUID constitutionVersionId;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InstitutionKind kind;

  public Institution(
      UUID polityId,
      UUID jurisdictionId,
      UUID constitutionVersionId,
      String name,
      InstitutionKind kind) {
    this.polityId = polityId;
    this.jurisdictionId = jurisdictionId;
    this.constitutionVersionId = constitutionVersionId;
    this.name = name;
    this.kind = kind;
  }
}
