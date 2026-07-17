package com.odonta.polity.resolver;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolityContextResolver {
  private final ConstitutionVersionRepository constitutions;
  private final JurisdictionRepository jurisdictions;
  private final PolityRepository polities;

  public ConstitutionVersion constitution(UUID polityId) {
    return constitutions
        .findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED)
        .orElseThrow(PolityResource.CONSTITUTION::notFound);
  }

  public Jurisdiction rootJurisdiction(UUID polityId) {
    return jurisdictions
        .findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT)
        .orElseThrow(PolityResource.JURISDICTION::notFound);
  }

  public String name(UUID polityId) {
    return polities
        .findProjectedById(polityId)
        .map(PolityProjection::getName)
        .orElseThrow(PolityResource.POLITY::notFound);
  }
}
