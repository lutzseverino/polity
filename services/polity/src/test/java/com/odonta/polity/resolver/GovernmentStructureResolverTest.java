package com.odonta.polity.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.mapper.GovernmentFormationApplicationMapper;
import com.odonta.polity.mapper.GovernmentStructureApplicationMapper;
import com.odonta.polity.mapper.JurisdictionApplicationMapper;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.JurisdictionProjection;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.result.ConstitutionResult;
import com.odonta.polity.service.ConstitutionService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

class GovernmentStructureResolverTest {
  private final ConstitutionService constitutions = mock(ConstitutionService.class);
  private final JurisdictionRepository jurisdictions = mock(JurisdictionRepository.class);
  private final GovernmentAssessmentResolver assessments = mock(GovernmentAssessmentResolver.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final GovernmentStructureResolver resolver =
      new GovernmentStructureResolver(
          constitutions,
          Mappers.getMapper(JurisdictionApplicationMapper.class),
          jurisdictions,
          Mappers.getMapper(GovernmentFormationApplicationMapper.class),
          assessments,
          polities,
          Mappers.getMapper(GovernmentStructureApplicationMapper.class));

  @Test
  void separatesVersionedConstitutionFromJurisdictionAndLiveFormationState() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ConstitutionResult constitution =
        new ConstitutionResult(
            UUID.randomUUID(),
            3,
            "Charter",
            "Rules",
            null,
            null,
            Map.of(),
            ConstitutionStatus.RATIFIED,
            OffsetDateTime.parse("2026-07-12T10:00:00Z"),
            List.of(),
            List.of(),
            List.of(),
            List.of());
    Polity polity = new Polity("Commons", PolityVisibility.PUBLIC, UUID.randomUUID());
    ReflectionTestUtils.setField(polity, "id", polityId);
    JurisdictionProjection jurisdiction = mock(JurisdictionProjection.class);
    when(jurisdiction.getId()).thenReturn(UUID.randomUUID());
    when(jurisdiction.getPolityId()).thenReturn(polityId);
    when(jurisdiction.getName()).thenReturn("Commons scope");
    when(jurisdiction.getKind()).thenReturn(JurisdictionKind.ROOT);
    when(constitutions.get(polityId, userId)).thenReturn(constitution);
    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(jurisdictions.findProjectionsByPolityId(polityId)).thenReturn(List.of(jurisdiction));
    when(assessments.minimumFullGovernmentMembers()).thenReturn(3);
    when(assessments.activeMemberCount(polityId)).thenReturn(2L);
    when(assessments.standingMemberCount(polityId)).thenReturn(1L);

    var result = resolver.resolve(polityId, userId);

    assertThat(result.constitution()).isSameAs(constitution);
    assertThat(result.jurisdictions())
        .singleElement()
        .extracting("name")
        .isEqualTo("Commons scope");
    assertThat(result.formation().minimumFullGovernmentMembers()).isEqualTo(3);
    assertThat(result.formation().activeMemberCount()).isEqualTo(2);
    assertThat(result.formation().standingMemberCount()).isEqualTo(1);
  }
}
