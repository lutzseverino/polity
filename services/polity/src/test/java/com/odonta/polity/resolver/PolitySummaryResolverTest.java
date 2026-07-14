package com.odonta.polity.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.mapper.PolityApplicationMapper;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.repository.ConstitutionVersionProjection;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.InstitutionProjection;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionProjection;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.PolityProjection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PolitySummaryResolverTest {
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final JurisdictionRepository jurisdictions = mock(JurisdictionRepository.class);
  private final InstitutionRepository institutions = mock(InstitutionRepository.class);
  private final PolitySummaryResolver resolver =
      new PolitySummaryResolver(
          Mappers.getMapper(PolityApplicationMapper.class),
          constitutions,
          jurisdictions,
          institutions);

  @Test
  void resolvesMultipleSummariesWithOneReadPerOwnedResource() {
    UUID firstPolityId = UUID.randomUUID();
    UUID secondPolityId = UUID.randomUUID();
    UUID firstConstitutionId = UUID.randomUUID();
    UUID secondConstitutionId = UUID.randomUUID();
    List<UUID> polityIds = List.of(firstPolityId, secondPolityId);
    List<UUID> constitutionIds = List.of(firstConstitutionId, secondConstitutionId);
    PolityProjection firstPolity = polity(firstPolityId, "First");
    PolityProjection secondPolity = polity(secondPolityId, "Second");
    List<ConstitutionVersionProjection> currentConstitutions =
        List.of(
            constitution(firstConstitutionId, firstPolityId, 1),
            constitution(secondConstitutionId, secondPolityId, 2));
    List<JurisdictionProjection> rootJurisdictions =
        List.of(
            jurisdiction(firstPolityId, "First jurisdiction"),
            jurisdiction(secondPolityId, "Second jurisdiction"));
    List<InstitutionProjection> currentInstitutions =
        List.of(
            council(firstConstitutionId, "First council"),
            council(secondConstitutionId, "Second council"));
    when(constitutions.findProjectionsByPolityIdInAndStatus(polityIds, ConstitutionStatus.RATIFIED))
        .thenReturn(currentConstitutions);
    when(jurisdictions.findProjectionsByPolityIdInAndKind(polityIds, JurisdictionKind.ROOT))
        .thenReturn(rootJurisdictions);
    when(institutions.findProjectionsByConstitutionVersionIdIn(constitutionIds))
        .thenReturn(currentInstitutions);

    var results = resolver.resolveAll(List.of(firstPolity, secondPolity));

    assertThat(results).extracting("name").containsExactly("First", "Second");
    assertThat(results).extracting("constitutionVersion").containsExactly(1, 2);
    assertThat(results)
        .extracting("institutionName")
        .containsExactly("First council", "Second council");
    verify(constitutions, times(1))
        .findProjectionsByPolityIdInAndStatus(polityIds, ConstitutionStatus.RATIFIED);
    verify(jurisdictions, times(1))
        .findProjectionsByPolityIdInAndKind(polityIds, JurisdictionKind.ROOT);
    verify(institutions, times(1)).findProjectionsByConstitutionVersionIdIn(constitutionIds);
  }

  @Test
  void usesAssemblyWhenExecutableConstitutionHasNoCouncil() {
    UUID polityId = UUID.randomUUID();
    UUID constitutionId = UUID.randomUUID();
    List<UUID> polityIds = List.of(polityId);
    List<UUID> constitutionIds = List.of(constitutionId);
    ConstitutionVersionProjection constitution = constitution(constitutionId, polityId, 3);
    JurisdictionProjection jurisdiction = jurisdiction(polityId, "Root");
    InstitutionProjection assembly =
        institution(constitutionId, "Assembly", InstitutionKind.ASSEMBLY);
    when(constitutions.findProjectionsByPolityIdInAndStatus(polityIds, ConstitutionStatus.RATIFIED))
        .thenReturn(List.of(constitution));
    when(jurisdictions.findProjectionsByPolityIdInAndKind(polityIds, JurisdictionKind.ROOT))
        .thenReturn(List.of(jurisdiction));
    when(institutions.findProjectionsByConstitutionVersionIdIn(constitutionIds))
        .thenReturn(List.of(assembly));

    var result = resolver.resolve(polity(polityId, "Assembly polity"));

    assertThat(result.institutionName()).isEqualTo("Assembly");
  }

  private PolityProjection polity(UUID id, String name) {
    PolityProjection projection = mock(PolityProjection.class);
    when(projection.getId()).thenReturn(id);
    when(projection.getName()).thenReturn(name);
    return projection;
  }

  private ConstitutionVersionProjection constitution(UUID id, UUID polityId, int version) {
    ConstitutionVersionProjection projection = mock(ConstitutionVersionProjection.class);
    when(projection.getId()).thenReturn(id);
    when(projection.getPolityId()).thenReturn(polityId);
    when(projection.getVersion()).thenReturn(version);
    return projection;
  }

  private JurisdictionProjection jurisdiction(UUID polityId, String name) {
    JurisdictionProjection projection = mock(JurisdictionProjection.class);
    when(projection.getPolityId()).thenReturn(polityId);
    when(projection.getName()).thenReturn(name);
    return projection;
  }

  private InstitutionProjection council(UUID constitutionId, String name) {
    return institution(constitutionId, name, InstitutionKind.COUNCIL);
  }

  private InstitutionProjection institution(
      UUID constitutionId, String name, InstitutionKind kind) {
    InstitutionProjection projection = mock(InstitutionProjection.class);
    when(projection.getId()).thenReturn(UUID.randomUUID());
    when(projection.getConstitutionVersionId()).thenReturn(constitutionId);
    when(projection.getName()).thenReturn(name);
    when(projection.getKind()).thenReturn(kind);
    return projection;
  }
}
