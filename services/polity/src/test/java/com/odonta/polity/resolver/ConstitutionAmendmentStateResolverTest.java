package com.odonta.polity.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConstitutionAmendmentStateResolverTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-12T12:00:00Z");

  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final InstitutionRepository institutions = mock(InstitutionRepository.class);
  private final JurisdictionRepository jurisdictions = mock(JurisdictionRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final ConstitutionAmendmentStateResolver resolver =
      new ConstitutionAmendmentStateResolver(
          powers, institutions, jurisdictions, offices, officeTerms, procedures);

  @Test
  void resolvesAnImmutableCurrentStateAndCountsOnlyUnexpiredActiveTerms() {
    UUID polityId = UUID.randomUUID();
    UUID constitutionId = UUID.randomUUID();
    ConstitutionVersion constitution = mock(ConstitutionVersion.class);
    when(constitution.getId()).thenReturn(constitutionId);
    when(constitution.getPolityId()).thenReturn(polityId);
    Institution institution = mock(Institution.class);
    when(institution.getId()).thenReturn(UUID.randomUUID());
    when(institution.getKind()).thenReturn(InstitutionKind.ASSEMBLY);
    Office office = mock(Office.class);
    when(office.getCode()).thenReturn("steward");
    Procedure procedure = mock(Procedure.class);
    UUID procedureInstitutionId = UUID.randomUUID();
    when(procedure.getCode()).thenReturn("ordinary-resolution");
    when(procedure.getInstitutionId()).thenReturn(procedureInstitutionId);
    ConstitutionalPower power = mock(ConstitutionalPower.class);
    when(power.getCode()).thenReturn(PowerCode.REQUEST_CERTIFICATION);
    when(power.getHolderScope()).thenReturn(PowerHolderScope.ACTIVE_MEMBER);
    Jurisdiction jurisdiction = mock(Jurisdiction.class);
    when(jurisdiction.getId()).thenReturn(UUID.randomUUID());
    OfficeTerm current = officeTerm("steward", NOW.plusDays(1));
    OfficeTerm expired = officeTerm("steward", NOW.minusSeconds(1));
    OfficeTerm boundary = officeTerm("steward", NOW);
    OfficeTerm otherOffice = officeTerm("tribune", NOW.plusDays(1));
    when(institutions.findEntitiesByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(institution));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(constitutionId))
        .thenReturn(List.of(office));
    when(procedures.findEntitiesByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(procedure));
    when(powers.findEntitiesByConstitutionVersionId(constitutionId)).thenReturn(List.of(power));
    when(jurisdictions.findEntitiesByPolityId(polityId)).thenReturn(List.of(jurisdiction));
    when(officeTerms.findEntitiesByPolityIdAndStatus(polityId, OfficeTermStatus.ACTIVE))
        .thenReturn(List.of(current, expired, boundary, otherOffice));

    var result = resolver.resolve(constitution, NOW);

    assertThat(result.institutionKinds()).containsOnlyKeys(institution.getId());
    assertThat(result.officeCodes()).containsOnly("steward");
    assertThat(result.procedures()).containsOnlyKeys("ordinary-resolution");
    assertThat(result.powers()).containsOnlyKeys(PowerCode.REQUEST_CERTIFICATION);
    assertThat(result.procedures().get("ordinary-resolution").institutionId())
        .isEqualTo(procedureInstitutionId);
    assertThat(result.powers().get(PowerCode.REQUEST_CERTIFICATION).holderScope())
        .isEqualTo(PowerHolderScope.ACTIVE_MEMBER);
    assertThat(result.jurisdictionIds()).containsOnly(jurisdiction.getId());
    assertThat(result.activeOfficeTermCounts())
        .containsExactlyInAnyOrderEntriesOf(java.util.Map.of("steward", 1L, "tribune", 1L));
    assertThatThrownBy(() -> result.officeCodes().add("new-office"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private OfficeTerm officeTerm(String officeCode, OffsetDateTime endsAt) {
    OfficeTerm term = mock(OfficeTerm.class);
    when(term.getOfficeCode()).thenReturn(officeCode);
    when(term.getEndsAt()).thenReturn(endsAt);
    return term;
  }
}
