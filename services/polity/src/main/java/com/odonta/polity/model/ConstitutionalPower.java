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
@Table(name = "constitutional_powers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConstitutionalPower extends AuditedEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "constitution_version_id", nullable = false)
  private UUID constitutionVersionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PowerCode code;

  @Column(nullable = false)
  private String name;

  @Column(name = "name_key")
  private String nameKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "holder_scope", nullable = false)
  private PowerHolderScope holderScope;

  @Column(name = "holder_office_code")
  private String holderOfficeCode;

  public ConstitutionalPower(
      UUID polityId,
      UUID constitutionVersionId,
      PowerCode code,
      String name,
      PowerHolderScope holderScope) {
    this(polityId, constitutionVersionId, code, name, null, holderScope);
  }

  public ConstitutionalPower(
      UUID polityId,
      UUID constitutionVersionId,
      PowerCode code,
      String name,
      ConstitutionalPowerTemplateKey templateKey,
      PowerHolderScope holderScope) {
    this.polityId = polityId;
    this.constitutionVersionId = constitutionVersionId;
    this.code = code;
    this.name = name;
    this.nameKey = templateKey == null ? null : templateKey.nameKey();
    this.holderScope = holderScope;
  }

  public ConstitutionalPower(
      UUID polityId,
      UUID constitutionVersionId,
      PowerCode code,
      String name,
      String holderOfficeCode) {
    this(polityId, constitutionVersionId, code, name, null, holderOfficeCode);
  }

  public ConstitutionalPower(
      UUID polityId,
      UUID constitutionVersionId,
      PowerCode code,
      String name,
      ConstitutionalPowerTemplateKey templateKey,
      String holderOfficeCode) {
    this.polityId = polityId;
    this.constitutionVersionId = constitutionVersionId;
    this.code = code;
    this.name = name;
    this.nameKey = templateKey == null ? null : templateKey.nameKey();
    this.holderScope = PowerHolderScope.OFFICE;
    this.holderOfficeCode = holderOfficeCode;
  }

  public ConstitutionalPower copyTo(UUID constitutionVersionId) {
    if (holderScope == PowerHolderScope.OFFICE) {
      return new ConstitutionalPower(
          polityId, constitutionVersionId, code, name, nameKey, holderScope, holderOfficeCode);
    }
    return new ConstitutionalPower(
        polityId, constitutionVersionId, code, name, nameKey, holderScope, null);
  }

  public ConstitutionalPower copyWithHolder(
      UUID constitutionVersionId, PowerHolderScope holderScope, String holderOfficeCode) {
    if (holderScope == PowerHolderScope.OFFICE) {
      return new ConstitutionalPower(
          polityId, constitutionVersionId, code, name, nameKey, holderScope, holderOfficeCode);
    }
    return new ConstitutionalPower(
        polityId, constitutionVersionId, code, name, nameKey, holderScope, null);
  }

  private ConstitutionalPower(
      UUID polityId,
      UUID constitutionVersionId,
      PowerCode code,
      String name,
      String nameKey,
      PowerHolderScope holderScope,
      String holderOfficeCode) {
    this.polityId = polityId;
    this.constitutionVersionId = constitutionVersionId;
    this.code = code;
    this.name = name;
    this.nameKey = nameKey;
    this.holderScope = holderScope;
    this.holderOfficeCode = holderOfficeCode;
  }
}
