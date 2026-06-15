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
@Table(name = "procedures")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Procedure extends AuditedEntity implements VotingProcedure {
  public static final String ORDINARY_RESOLUTION = "ordinary-resolution";

  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "constitution_version_id", nullable = false)
  private UUID constitutionVersionId;

  @Column(name = "institution_id", nullable = false)
  private UUID institutionId;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(name = "quorum_numerator", nullable = false)
  private int quorumNumerator;

  @Column(name = "quorum_denominator", nullable = false)
  private int quorumDenominator;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VotingThreshold threshold;

  @Enumerated(EnumType.STRING)
  @Column(name = "effect_type", nullable = false)
  private EffectType effectType;

  public Procedure(
      UUID polityId,
      UUID constitutionVersionId,
      UUID institutionId,
      String code,
      String name,
      int quorumNumerator,
      int quorumDenominator,
      VotingThreshold threshold,
      EffectType effectType) {
    this.polityId = polityId;
    this.constitutionVersionId = constitutionVersionId;
    this.institutionId = institutionId;
    this.code = code;
    this.name = name;
    this.quorumNumerator = quorumNumerator;
    this.quorumDenominator = quorumDenominator;
    this.threshold = threshold;
    this.effectType = effectType;
  }
}
