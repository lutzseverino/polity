package com.odonta.polity.model;

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
@Table(name = "official_record_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialRecordEntry {
  @Id @GeneratedValue private UUID id;

  @Column(name = "polity_id", nullable = false)
  private UUID polityId;

  @Column(name = "jurisdiction_id", nullable = false)
  private UUID jurisdictionId;

  @Column(name = "constitution_version_id", nullable = false)
  private UUID constitutionVersionId;

  @Column(name = "actor_membership_id", nullable = false)
  private UUID actorMembershipId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OfficialRecordType type;

  @Column(name = "source_id")
  private UUID sourceId;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String body;

  @Column(name = "occurred_at", nullable = false)
  private OffsetDateTime occurredAt;

  public OfficialRecordEntry(
      UUID polityId,
      UUID jurisdictionId,
      UUID constitutionVersionId,
      UUID actorMembershipId,
      OfficialRecordType type,
      UUID sourceId,
      String title,
      String body,
      OffsetDateTime occurredAt) {
    this.polityId = polityId;
    this.jurisdictionId = jurisdictionId;
    this.constitutionVersionId = constitutionVersionId;
    this.actorMembershipId = actorMembershipId;
    this.type = type;
    this.sourceId = sourceId;
    this.title = title;
    this.body = body;
    this.occurredAt = occurredAt;
  }
}
