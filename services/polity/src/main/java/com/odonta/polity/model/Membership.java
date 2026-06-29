package com.odonta.polity.model;

import com.odonta.common.data.AuditedEntity;
import com.odonta.common.data.PersonalDataEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "memberships")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership extends AuditedEntity implements PersonalDataEntity {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "authorization_subject", nullable = false)
  private String authorizationSubject;

  @Column(nullable = false)
  private String email;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MembershipStatus status;

  @Column(name = "admitted_at", nullable = false)
  private OffsetDateTime admittedAt;

  @Column(name = "admitted_by")
  private UUID admittedBy;

  @Column(name = "resigned_at")
  private OffsetDateTime resignedAt;

  public Membership(
      UUID polityId,
      UUID userId,
      String authorizationSubject,
      String email,
      String displayName,
      OffsetDateTime admittedAt,
      UUID admittedBy) {
    this.polityId = polityId;
    this.userId = userId;
    this.authorizationSubject = authorizationSubject;
    this.email = email;
    this.displayName = displayName;
    this.status = MembershipStatus.ACTIVE;
    this.admittedAt = admittedAt;
    this.admittedBy = admittedBy;
  }

  public void resign(OffsetDateTime resignedAt) {
    if (status != MembershipStatus.ACTIVE) {
      throw new IllegalStateException("Only active memberships can resign");
    }
    this.status = MembershipStatus.RESIGNED;
    this.resignedAt = resignedAt;
  }

  public void reactivate(
      String authorizationSubject,
      String email,
      String displayName,
      OffsetDateTime admittedAt,
      UUID admittedBy) {
    if (status == MembershipStatus.ACTIVE) {
      throw new IllegalStateException("Only inactive memberships can be reactivated");
    }
    this.authorizationSubject = authorizationSubject;
    this.email = email;
    this.displayName = displayName;
    this.status = MembershipStatus.ACTIVE;
    this.admittedAt = admittedAt;
    this.admittedBy = admittedBy;
    this.resignedAt = null;
  }
}
