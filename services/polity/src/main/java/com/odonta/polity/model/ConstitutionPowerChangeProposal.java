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
@Table(name = "constitution_power_change_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConstitutionPowerChangeProposal extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "amendment_proposal_id", nullable = false)
  private UUID amendmentProposalId;

  @Enumerated(EnumType.STRING)
  @Column(name = "power_code", nullable = false)
  private PowerCode powerCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "holder_scope", nullable = false)
  private PowerHolderScope holderScope;

  @Column(name = "holder_office_code")
  private String holderOfficeCode;

  public ConstitutionPowerChangeProposal(
      UUID polityId,
      UUID amendmentProposalId,
      PowerCode powerCode,
      PowerHolderScope holderScope,
      String holderOfficeCode) {
    this.polityId = polityId;
    this.amendmentProposalId = amendmentProposalId;
    this.powerCode = powerCode;
    this.holderScope = holderScope;
    this.holderOfficeCode = holderOfficeCode;
  }
}
