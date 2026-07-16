package com.odonta.polity.model;

import io.github.lutzseverino.cardo.common.data.AuditedEntity;
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
@Table(name = "constitution_institution_change_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConstitutionInstitutionChangeProposal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "amendment_proposal_id", nullable = false)
  private UUID amendmentProposalId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ConstitutionChangeOperation action;

  @Column(name = "institution_id")
  private UUID institutionId;

  @Column(name = "jurisdiction_id")
  private UUID jurisdictionId;

  @Column private String name;

  @Enumerated(EnumType.STRING)
  @Column
  private InstitutionKind kind;

  public ConstitutionInstitutionChangeProposal(
      UUID polityId,
      UUID amendmentProposalId,
      ConstitutionChangeOperation action,
      UUID institutionId,
      UUID jurisdictionId,
      String name,
      InstitutionKind kind) {
    this.polityId = polityId;
    this.amendmentProposalId = amendmentProposalId;
    this.action = action;
    this.institutionId = institutionId;
    this.jurisdictionId = jurisdictionId;
    this.name = name;
    this.kind = kind;
  }
}
