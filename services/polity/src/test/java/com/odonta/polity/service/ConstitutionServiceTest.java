package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.ConstitutionApplicationMapper;
import com.odonta.polity.mapper.ConstitutionalPowerApplicationMapper;
import com.odonta.polity.mapper.InstitutionApplicationMapper;
import com.odonta.polity.mapper.OfficeApplicationMapper;
import com.odonta.polity.mapper.ProcedureApplicationMapper;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.InstitutionTemplateKey;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureTemplateKey;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerProjection;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionProjection;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.ProcedureProjection;
import com.odonta.polity.repository.ProcedureRepository;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

class ConstitutionServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  @Test
  void getExposesStructuredCharter() {
    UUID polityId = UUID.randomUUID();
    UUID constitutionId = UUID.randomUUID();
    UUID jurisdictionId = UUID.randomUUID();
    ConstitutionVersionRepository constitutions = mock(ConstitutionVersionRepository.class);
    InstitutionRepository institutions = mock(InstitutionRepository.class);
    ProcedureRepository procedures = mock(ProcedureRepository.class);
    OfficeRepository offices = mock(OfficeRepository.class);
    ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", jurisdictionId);
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            polityId,
            1,
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(),
            ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
            NOW.minusDays(1));
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    Institution institution =
        new Institution(
            polityId,
            jurisdictionId,
            constitutionId,
            InstitutionTemplateKey.CITIZENS_ASSEMBLY.storedName(),
            InstitutionTemplateKey.CITIZENS_ASSEMBLY,
            InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(institution, "id", UUID.randomUUID());
    Office steward =
        new Office(
            polityId,
            constitutionId,
            jurisdictionId,
            Office.STEWARD,
            "Steward",
            "Coordinates proceedings.",
            14);
    Procedure procedure =
        new Procedure(
            polityId,
            constitutionId,
            institution.getId(),
            Procedure.ORDINARY_RESOLUTION,
            ProcedureTemplateKey.ORDINARY_RESOLUTION.storedName(),
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            0,
            48,
            EffectType.ADOPT_RESOLUTION);
    ConstitutionalPower power =
        new ConstitutionalPower(
            polityId,
            constitutionId,
            PowerCode.INTRODUCE_MOTION,
            "Introduce motions",
            PowerHolderScope.ACTIVE_MEMBER);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(institutions.findProjectionsByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(projection(InstitutionProjection.class, institution)));
    when(procedures.findProjectionsByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(projection(ProcedureProjection.class, procedure)));
    when(offices.findProjectionsByConstitutionVersionIdOrderByNameAscIdAsc(constitutionId))
        .thenReturn(List.of(projection(OfficeProjection.class, steward)));
    when(powers.findProjectionsByConstitutionVersionId(constitutionId))
        .thenReturn(List.of(projection(ConstitutionalPowerProjection.class, power)));
    ConstitutionService service =
        new ConstitutionService(
            mock(PolityAccessPolicy.class),
            Mappers.getMapper(ConstitutionApplicationMapper.class),
            constitutions,
            Mappers.getMapper(InstitutionApplicationMapper.class),
            institutions,
            Mappers.getMapper(ProcedureApplicationMapper.class),
            procedures,
            Mappers.getMapper(OfficeApplicationMapper.class),
            offices,
            Mappers.getMapper(ConstitutionalPowerApplicationMapper.class),
            powers);

    var result = service.get(polityId, UUID.randomUUID());

    assertThat(result.version()).isEqualTo(1);
    assertThat(result.title()).isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle());
    assertThat(result.body()).isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody());
    assertThat(result.institutions())
        .singleElement()
        .satisfies(
            found -> {
              assertThat(found.name())
                  .isEqualTo(InstitutionTemplateKey.CITIZENS_ASSEMBLY.storedName());
              assertThat(found.nameKey())
                  .isEqualTo(InstitutionTemplateKey.CITIZENS_ASSEMBLY.nameKey());
            });
    assertThat(result.procedures())
        .singleElement()
        .extracting("code")
        .isEqualTo(Procedure.ORDINARY_RESOLUTION);
    assertThat(result.offices()).singleElement().extracting("code").isEqualTo(Office.STEWARD);
    assertThat(result.powers())
        .singleElement()
        .extracting("code")
        .isEqualTo(PowerCode.INTRODUCE_MOTION);
  }

  private static <T> T projection(Class<T> type, Object source) {
    Object proxy =
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (ignored, method, args) -> {
              if (method.getDeclaringClass() == Object.class) {
                return method.invoke(source, args);
              }
              return source.getClass().getMethod(method.getName()).invoke(source);
            });
    return type.cast(proxy);
  }
}
