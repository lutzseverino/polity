package com.odonta.polity.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolityContextResolverTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-16T10:00:00Z");

  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final JurisdictionRepository jurisdictions = mock(JurisdictionRepository.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final PolityContextResolver resolver =
      new PolityContextResolver(constitutions, jurisdictions, polities);

  @Test
  void constitutionReturnsTheRatifiedConstitutionForThePolity() {
    UUID polityId = UUID.randomUUID();
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Constitution", "Body", NOW);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));

    assertThat(resolver.constitution(polityId)).isSameAs(constitution);

    verify(constitutions).findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED);
  }

  @Test
  void constitutionUsesTheCanonicalMissingConstitutionProblem() {
    UUID polityId = UUID.randomUUID();
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> resolver.constitution(polityId))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> {
              assertThat(exception.code()).isEqualTo("constitution_not_found");
              assertThat(exception.getMessage()).isEqualTo("Constitution not found.");
            });
  }

  @Test
  void rootJurisdictionReturnsTheRootJurisdictionForThePolity() {
    UUID polityId = UUID.randomUUID();
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));

    assertThat(resolver.rootJurisdiction(polityId)).isSameAs(jurisdiction);

    verify(jurisdictions).findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT);
  }

  @Test
  void rootJurisdictionUsesTheCanonicalMissingJurisdictionProblem() {
    UUID polityId = UUID.randomUUID();
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> resolver.rootJurisdiction(polityId))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> {
              assertThat(exception.code()).isEqualTo("jurisdiction_not_found");
              assertThat(exception.getMessage()).isEqualTo("Jurisdiction not found.");
            });
  }

  @Test
  void nameReturnsTheProjectedPolityName() {
    UUID polityId = UUID.randomUUID();
    PolityProjection projection = mock(PolityProjection.class);
    when(projection.getName()).thenReturn("Friend Republic");
    when(polities.findProjectedById(polityId)).thenReturn(Optional.of(projection));

    assertThat(resolver.name(polityId)).isEqualTo("Friend Republic");

    verify(polities).findProjectedById(polityId);
  }

  @Test
  void nameUsesTheCanonicalMissingPolityProblem() {
    UUID polityId = UUID.randomUUID();
    when(polities.findProjectedById(polityId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> resolver.name(polityId))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> {
              assertThat(exception.code()).isEqualTo("polity_not_found");
              assertThat(exception.getMessage()).isEqualTo("Polity not found.");
            });
  }
}
