package com.odonta.polity.model;

import com.odonta.polity.PolityResources;
import io.github.lutzseverino.cardo.authorization.resource.AuthorizationResourceType;
import io.github.lutzseverino.cardo.authorization.resource.TargetableAuthorizationResource;
import io.github.lutzseverino.cardo.common.data.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "polities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Polity extends AuditedEntity implements TargetableAuthorizationResource {
  @Id @GeneratedValue private UUID id;

  @Column(name = "founder_id", nullable = false)
  private UUID founderId;

  @NotBlank @Size(max = 120) @Column(nullable = false)
  private String name;

  @NotBlank @Size(max = 80) @Column(nullable = false, unique = true, updatable = false)
  private String slug;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PolityVisibility visibility;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PolityStatus status = PolityStatus.ACTIVE;

  @Column(name = "disbanded_at")
  private OffsetDateTime disbandedAt;

  @Column(name = "bootstrap_completed_at")
  private OffsetDateTime bootstrapCompletedAt;

  public Polity(String name, String slug, PolityVisibility visibility, UUID founderId) {
    this.name = name;
    this.slug = slug;
    this.visibility = visibility;
    this.founderId = founderId;
  }

  public boolean isActive() {
    return status == PolityStatus.ACTIVE;
  }

  public boolean isDisbanded() {
    return status == PolityStatus.DISBANDED;
  }

  public void disband(OffsetDateTime disbandedAt) {
    if (status != PolityStatus.ACTIVE) {
      throw new IllegalStateException("Only active polities can be disbanded");
    }
    status = PolityStatus.DISBANDED;
    this.disbandedAt = disbandedAt;
  }

  public boolean isBootstrapComplete() {
    return bootstrapCompletedAt != null;
  }

  public void completeBootstrap(OffsetDateTime completedAt) {
    if (bootstrapCompletedAt == null) {
      bootstrapCompletedAt = completedAt;
    }
  }

  @Override
  public AuthorizationResourceType authorizationResourceType() {
    return PolityResources.POLITY;
  }

  @Override
  public UUID authorizationResourceId() {
    return id;
  }
}
