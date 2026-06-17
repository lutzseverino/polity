package com.odonta.polity.model;

import com.odonta.authorization.resource.AuthorizationResourceType;
import com.odonta.authorization.resource.TargetableAuthorizationResource;
import com.odonta.common.data.AuditedEntity;
import com.odonta.polity.PolityResources;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PolityVisibility visibility;

  public Polity(String name, PolityVisibility visibility, UUID founderId) {
    this.name = name;
    this.visibility = visibility;
    this.founderId = founderId;
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
