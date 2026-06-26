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
@Table(name = "constitution_office_change_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConstitutionOfficeChangeProposal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "amendment_proposal_id", nullable = false)
  private UUID amendmentProposalId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ConstitutionOfficeChangeAction action;

  @Column(name = "office_code", nullable = false)
  private String officeCode;

  @Column(name = "jurisdiction_id")
  private UUID jurisdictionId;

  @Column private String name;

  @Column private String description;

  @Column(name = "term_length_days")
  private Integer termLengthDays;

  @Column(name = "seat_count")
  private Integer seatCount;

  public ConstitutionOfficeChangeProposal(
      UUID polityId,
      UUID amendmentProposalId,
      ConstitutionOfficeChangeAction action,
      String officeCode,
      UUID jurisdictionId,
      String name,
      String description,
      Integer termLengthDays,
      Integer seatCount) {
    this.polityId = polityId;
    this.amendmentProposalId = amendmentProposalId;
    this.action = action;
    this.officeCode = officeCode;
    this.jurisdictionId = jurisdictionId;
    this.name = name;
    this.description = description;
    this.termLengthDays = termLengthDays;
    this.seatCount = seatCount;
  }
}
