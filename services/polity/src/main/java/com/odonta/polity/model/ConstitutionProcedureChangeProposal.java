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
@Table(name = "constitution_procedure_change_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConstitutionProcedureChangeProposal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "amendment_proposal_id", nullable = false)
  private UUID amendmentProposalId;

  @Column(name = "procedure_code", nullable = false)
  private String procedureCode;

  @Column(name = "institution_id")
  private UUID institutionId;

  @Column(name = "quorum_numerator")
  private Integer quorumNumerator;

  @Column(name = "quorum_denominator")
  private Integer quorumDenominator;

  @Enumerated(EnumType.STRING)
  @Column(name = "threshold")
  private VotingThreshold threshold;

  @Enumerated(EnumType.STRING)
  @Column(name = "electorate")
  private ProcedureElectorate electorate;

  @Column(name = "electorate_office_code")
  private String electorateOfficeCode;

  @Column(name = "minimum_elector_count")
  private Integer minimumElectorCount;

  @Column(name = "minimum_notice_hours")
  private Integer minimumNoticeHours;

  @Column(name = "voting_period_hours")
  private Integer votingPeriodHours;

  public ConstitutionProcedureChangeProposal(
      UUID polityId,
      UUID amendmentProposalId,
      String procedureCode,
      UUID institutionId,
      Integer quorumNumerator,
      Integer quorumDenominator,
      VotingThreshold threshold,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      Integer minimumElectorCount,
      Integer minimumNoticeHours,
      Integer votingPeriodHours) {
    this.polityId = polityId;
    this.amendmentProposalId = amendmentProposalId;
    this.procedureCode = procedureCode;
    this.institutionId = institutionId;
    this.quorumNumerator = quorumNumerator;
    this.quorumDenominator = quorumDenominator;
    this.threshold = threshold;
    this.electorate = electorate;
    this.electorateOfficeCode = electorateOfficeCode;
    this.minimumElectorCount = minimumElectorCount;
    this.minimumNoticeHours = minimumNoticeHours;
    this.votingPeriodHours = votingPeriodHours;
  }
}
