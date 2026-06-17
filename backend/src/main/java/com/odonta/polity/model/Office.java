package com.odonta.polity.model;

import com.odonta.common.data.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "offices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Office extends AuditedEntity {
  public static final String STEWARD = "steward";

  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "constitution_version_id", nullable = false)
  private UUID constitutionVersionId;

  @Column(name = "jurisdiction_id", nullable = false)
  private UUID jurisdictionId;

  @NotBlank @Column(nullable = false)
  private String code;

  @NotBlank @Column(nullable = false)
  private String name;

  @NotBlank @Column(nullable = false)
  private String description;

  @Positive @Column(name = "term_length_days", nullable = false)
  private int termLengthDays;

  public Office(
      UUID polityId,
      UUID constitutionVersionId,
      UUID jurisdictionId,
      String code,
      String name,
      String description,
      int termLengthDays) {
    this.polityId = polityId;
    this.constitutionVersionId = constitutionVersionId;
    this.jurisdictionId = jurisdictionId;
    this.code = code;
    this.name = name;
    this.description = description;
    this.termLengthDays = termLengthDays;
  }

  public Office copyTo(UUID constitutionVersionId) {
    return new Office(
        polityId, constitutionVersionId, jurisdictionId, code, name, description, termLengthDays);
  }
}
