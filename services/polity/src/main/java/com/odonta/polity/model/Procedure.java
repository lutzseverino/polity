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
  public static final String OFFICE_ELECTION = "office-election";
  public static final String SANCTION = "sanction";
  public static final String APPEAL = "appeal";
  public static final String OFFICE_TERM_REVIEW = "office-term-review";
  public static final String CONSTITUTIONAL_REVIEW = "constitutional-review";
  public static final String CONSTITUTION_AMENDMENT = "constitution-amendment";
  public static final String DISBANDMENT = "disbandment";

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

  @Column(name = "name_key")
  private String nameKey;

  @Column(name = "quorum_numerator", nullable = false)
  private int quorumNumerator;

  @Column(name = "quorum_denominator", nullable = false)
  private int quorumDenominator;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VotingThreshold threshold;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProcedureElectorate electorate;

  @Column(name = "electorate_office_code")
  private String electorateOfficeCode;

  @Column(name = "minimum_elector_count", nullable = false)
  private int minimumElectorCount;

  @Column(name = "minimum_notice_hours", nullable = false)
  private int minimumNoticeHours;

  @Column(name = "voting_period_hours", nullable = false)
  private int votingPeriodHours;

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
      int minimumNoticeHours,
      int votingPeriodHours,
      EffectType effectType) {
    this(
        polityId,
        constitutionVersionId,
        institutionId,
        code,
        name,
        (String) null,
        quorumNumerator,
        quorumDenominator,
        threshold,
        ProcedureElectorate.ACTIVE_MEMBERS,
        null,
        1,
        minimumNoticeHours,
        votingPeriodHours,
        effectType);
  }

  public Procedure(
      UUID polityId,
      UUID constitutionVersionId,
      UUID institutionId,
      String code,
      String name,
      ProcedureTemplateKey templateKey,
      int quorumNumerator,
      int quorumDenominator,
      VotingThreshold threshold,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      int minimumNoticeHours,
      int votingPeriodHours,
      EffectType effectType) {
    this(
        polityId,
        constitutionVersionId,
        institutionId,
        code,
        name,
        templateKey,
        quorumNumerator,
        quorumDenominator,
        threshold,
        electorate,
        electorateOfficeCode,
        1,
        minimumNoticeHours,
        votingPeriodHours,
        effectType);
  }

  public Procedure(
      UUID polityId,
      UUID constitutionVersionId,
      UUID institutionId,
      String code,
      String name,
      ProcedureTemplateKey templateKey,
      int quorumNumerator,
      int quorumDenominator,
      VotingThreshold threshold,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      int minimumElectorCount,
      int minimumNoticeHours,
      int votingPeriodHours,
      EffectType effectType) {
    this(
        polityId,
        constitutionVersionId,
        institutionId,
        code,
        name,
        templateKey == null ? null : templateKey.nameKey(),
        quorumNumerator,
        quorumDenominator,
        threshold,
        electorate,
        electorateOfficeCode,
        minimumElectorCount,
        minimumNoticeHours,
        votingPeriodHours,
        effectType);
  }

  public Procedure(
      UUID polityId,
      UUID constitutionVersionId,
      UUID institutionId,
      String code,
      String name,
      ProcedureTemplateKey templateKey,
      int quorumNumerator,
      int quorumDenominator,
      VotingThreshold threshold,
      int minimumNoticeHours,
      int votingPeriodHours,
      EffectType effectType) {
    this(
        polityId,
        constitutionVersionId,
        institutionId,
        code,
        name,
        templateKey,
        quorumNumerator,
        quorumDenominator,
        threshold,
        ProcedureElectorate.ACTIVE_MEMBERS,
        null,
        1,
        minimumNoticeHours,
        votingPeriodHours,
        effectType);
  }

  public Procedure copyTo(UUID constitutionVersionId, UUID institutionId) {
    return copyWithRules(
        constitutionVersionId,
        institutionId,
        quorumNumerator,
        quorumDenominator,
        threshold,
        electorate,
        electorateOfficeCode,
        minimumElectorCount,
        minimumNoticeHours,
        votingPeriodHours);
  }

  public Procedure copyWithRules(
      UUID constitutionVersionId,
      UUID institutionId,
      int quorumNumerator,
      int quorumDenominator,
      VotingThreshold threshold,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      int minimumElectorCount,
      int minimumNoticeHours,
      int votingPeriodHours) {
    return new Procedure(
        polityId,
        constitutionVersionId,
        institutionId,
        code,
        name,
        nameKey,
        quorumNumerator,
        quorumDenominator,
        threshold,
        electorate,
        electorateOfficeCode,
        minimumElectorCount,
        minimumNoticeHours,
        votingPeriodHours,
        effectType);
  }

  private Procedure(
      UUID polityId,
      UUID constitutionVersionId,
      UUID institutionId,
      String code,
      String name,
      String nameKey,
      int quorumNumerator,
      int quorumDenominator,
      VotingThreshold threshold,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      int minimumElectorCount,
      int minimumNoticeHours,
      int votingPeriodHours,
      EffectType effectType) {
    this.polityId = polityId;
    this.constitutionVersionId = constitutionVersionId;
    this.institutionId = institutionId;
    this.code = code;
    this.name = name;
    this.nameKey = nameKey;
    this.quorumNumerator = quorumNumerator;
    this.quorumDenominator = quorumDenominator;
    this.threshold = threshold;
    this.electorate = electorate;
    this.electorateOfficeCode = electorateOfficeCode;
    this.minimumElectorCount = minimumElectorCount;
    this.minimumNoticeHours = minimumNoticeHours;
    this.votingPeriodHours = votingPeriodHours;
    this.effectType = effectType;
  }
}
