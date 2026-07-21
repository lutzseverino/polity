package com.odonta.polity.model;

import io.github.lutzseverino.cardo.common.data.AuditedEntity;
import io.github.lutzseverino.cardo.common.data.PersonalDataEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "polity_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolityAccount extends AuditedEntity implements PersonalDataEntity {
  @Id
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "authorization_subject", nullable = false, updatable = false)
  private String authorizationSubject;

  @Column(name = "grant_receipt_id", nullable = false, unique = true, updatable = false)
  private UUID grantReceiptId;

  public PolityAccount(UUID userId, String authorizationSubject, UUID grantReceiptId) {
    this.userId = userId;
    this.authorizationSubject = authorizationSubject;
    this.grantReceiptId = grantReceiptId;
  }
}
