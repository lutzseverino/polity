package com.odonta.polity.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionInstitutionChangeAction;
import com.odonta.polity.model.ConstitutionInstitutionChangeProposal;
import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import com.odonta.polity.model.ConstitutionPowerChangeProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.ConstitutionAmendmentProposalProjection;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.service.OfficialRecordService;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ConstitutionAmendmentEffectTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-14T10:00:00Z");

  private final ConstitutionAmendmentProposalRepository amendmentProposals =
      mock(ConstitutionAmendmentProposalRepository.class);
  private final ConstitutionInstitutionChangeProposalRepository institutionChangeProposals =
      mock(ConstitutionInstitutionChangeProposalRepository.class);
  private final ConstitutionOfficeChangeProposalRepository officeChangeProposals =
      mock(ConstitutionOfficeChangeProposalRepository.class);
  private final ConstitutionPowerChangeProposalRepository powerChangeProposals =
      mock(ConstitutionPowerChangeProposalRepository.class);
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals =
      mock(ConstitutionProcedureChangeProposalRepository.class);
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final InstitutionRepository institutions = mock(InstitutionRepository.class);
  private final JurisdictionRepository jurisdictions = mock(JurisdictionRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);

  private final ConstitutionAmendmentEffect effect =
      new ConstitutionAmendmentEffect(
          amendmentProposals,
          institutionChangeProposals,
          officeChangeProposals,
          powerChangeProposals,
          procedureChangeProposals,
          constitutions,
          powers,
          institutions,
          jurisdictions,
          offices,
          officeTerms,
          officialRecords,
          procedures);

  @Test
  void amendmentSupersedesCurrentConstitutionAndCopiesKernelRows() {
    UUID polityId = UUID.randomUUID();
    UUID currentConstitutionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    UUID procedureId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion current = constitution(polityId, currentConstitutionId, "Old body");
    Motion motion = motion(polityId, actor.getId(), currentConstitutionId);
    ConstitutionAmendmentProposal proposal =
        new ConstitutionAmendmentProposal(
            polityId, motion.getId(), "Second Constitution", "New body");
    ReflectionTestUtils.setField(proposal, "id", UUID.randomUUID());
    ConstitutionProcedureChangeProposal procedureChange =
        new ConstitutionProcedureChangeProposal(
            polityId,
            proposal.getId(),
            Procedure.ORDINARY_RESOLUTION,
            null,
            null,
            null,
            VotingThreshold.TWO_THIRDS_CAST,
            null,
            ProcedureElectorate.ACTIVE_MEMBERS,
            null,
            null,
            null,
            null);
    Institution institution =
        new Institution(
            polityId,
            motion.getJurisdictionId(),
            currentConstitutionId,
            "Citizens' Assembly",
            InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(institution, "id", institutionId);
    Procedure procedure =
        new Procedure(
            polityId,
            currentConstitutionId,
            institutionId,
            Procedure.ORDINARY_RESOLUTION,
            "Ordinary resolution",
            null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            0,
            24,
            EffectType.ADOPT_RESOLUTION);
    ReflectionTestUtils.setField(procedure, "id", procedureId);

    when(amendmentProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionAmendmentProposalProjection.class, proposal)));
    when(procedureChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(
            List.of(
                projection(ConstitutionProcedureChangeProposalProjection.class, procedureChange)));
    when(institutionChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(officeChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(powerChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(constitutions.findEntityById(currentConstitutionId)).thenReturn(Optional.of(current));
    when(constitutions.saveAndFlush(any(ConstitutionVersion.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(institutions.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(institution));
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(procedures.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(procedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(currentConstitutionId))
        .thenReturn(List.of());
    when(powers.findEntitiesByConstitutionVersionId(currentConstitutionId)).thenReturn(List.of());

    effect.apply(motion, actor, current, NOW);

    assertThat(current.getStatus()).isEqualTo(ConstitutionStatus.SUPERSEDED);
    ArgumentCaptor<ConstitutionVersion> constitutionCaptor =
        ArgumentCaptor.forClass(ConstitutionVersion.class);
    verify(constitutions, org.mockito.Mockito.times(2)).saveAndFlush(constitutionCaptor.capture());
    ConstitutionVersion amended = constitutionCaptor.getAllValues().get(1);
    assertThat(amended.getVersion()).isEqualTo(2);
    assertThat(amended.getTitle())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(2));
    assertThat(amended.getBody())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody());
    assertThat(amended.getTitleKey())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.titleKey());
    assertThat(amended.getBodyKey())
        .isEqualTo(ConstitutionTemplateKey.STRUCTURED_CHARTER.bodyKey());
    ArgumentCaptor<Procedure> procedureCaptor = ArgumentCaptor.forClass(Procedure.class);
    verify(procedures).save(procedureCaptor.capture());
    assertThat(procedureCaptor.getValue().getThreshold())
        .isEqualTo(VotingThreshold.TWO_THIRDS_CAST);
    assertThat(procedureCaptor.getValue().getElectorate())
        .isEqualTo(ProcedureElectorate.ACTIVE_MEMBERS);
    assertThat(procedureCaptor.getValue().getElectorateOfficeCode()).isNull();
    ArgumentCaptor<OfficialRecordTemplate> recordTemplate =
        ArgumentCaptor.forClass(OfficialRecordTemplate.class);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.CONSTITUTION_AMENDED),
            any(),
            any(),
            recordTemplate.capture(),
            any());
    assertThat(recordTemplate.getValue().params())
        .containsEntry("changeCount", 1)
        .containsEntry("amendmentBody", "New body");
    assertThat((List<?>) recordTemplate.getValue().params().get("changeItems"))
        .singleElement()
        .satisfies(
            item -> {
              Map<?, ?> changeItem = (Map<?, ?>) item;
              assertThat(changeItem.get("kind")).isEqualTo("procedure");
              assertThat(changeItem.get("operation")).isEqualTo("revise");
              assertThat(changeItem.get("subject")).isEqualTo("Ordinary resolution");
              Map<?, ?> details = (Map<?, ?>) changeItem.get("details");
              assertThat(details.get("threshold")).isEqualTo(VotingThreshold.TWO_THIRDS_CAST);
            });
  }

  @Test
  void amendmentCreatesOfficeAndReassignsPowerInNewConstitution() {
    UUID polityId = UUID.randomUUID();
    UUID currentConstitutionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    UUID jurisdictionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion current = constitution(polityId, currentConstitutionId, "Old body");
    Jurisdiction jurisdiction = jurisdiction(polityId, jurisdictionId);
    Motion motion = motion(polityId, actor.getId(), currentConstitutionId);
    ConstitutionAmendmentProposal proposal =
        new ConstitutionAmendmentProposal(
            polityId, motion.getId(), "Second Constitution", "New body");
    ReflectionTestUtils.setField(proposal, "id", UUID.randomUUID());
    Institution institution =
        new Institution(
            polityId,
            jurisdictionId,
            currentConstitutionId,
            "Citizens' Assembly",
            InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(institution, "id", institutionId);
    Office steward =
        new Office(
            polityId,
            currentConstitutionId,
            jurisdictionId,
            Office.STEWARD,
            "Steward",
            "Coordinates proceedings.",
            14);
    ConstitutionalPower admitMember =
        new ConstitutionalPower(
            polityId,
            currentConstitutionId,
            PowerCode.ADMIT_MEMBER,
            "Admit citizens",
            Office.STEWARD);
    ConstitutionOfficeChangeProposal officeChange =
        new ConstitutionOfficeChangeProposal(
            polityId,
            proposal.getId(),
            ConstitutionOfficeChangeAction.CREATE,
            "clerk",
            null,
            "Clerk",
            "Keeps the rolls.",
            30,
            1);
    ConstitutionPowerChangeProposal powerChange =
        new ConstitutionPowerChangeProposal(
            polityId, proposal.getId(), PowerCode.ADMIT_MEMBER, PowerHolderScope.OFFICE, "clerk");

    when(amendmentProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionAmendmentProposalProjection.class, proposal)));
    when(procedureChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(institutionChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(officeChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(
            List.of(projection(ConstitutionOfficeChangeProposalProjection.class, officeChange)));
    when(powerChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(
            List.of(projection(ConstitutionPowerChangeProposalProjection.class, powerChange)));
    when(constitutions.findEntityById(currentConstitutionId)).thenReturn(Optional.of(current));
    when(constitutions.saveAndFlush(any(ConstitutionVersion.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(institutions.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(institution));
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(procedures.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of());
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(currentConstitutionId))
        .thenReturn(List.of(steward));
    when(powers.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(admitMember));

    effect.apply(motion, actor, current, NOW);

    ArgumentCaptor<Office> officeCaptor = ArgumentCaptor.forClass(Office.class);
    verify(offices, org.mockito.Mockito.times(2)).save(officeCaptor.capture());
    assertThat(officeCaptor.getAllValues())
        .extracting(Office::getCode)
        .containsExactlyInAnyOrder(Office.STEWARD, "clerk");
    ArgumentCaptor<ConstitutionalPower> powerCaptor =
        ArgumentCaptor.forClass(ConstitutionalPower.class);
    verify(powers).save(powerCaptor.capture());
    assertThat(powerCaptor.getValue().getHolderScope()).isEqualTo(PowerHolderScope.OFFICE);
    assertThat(powerCaptor.getValue().getHolderOfficeCode()).isEqualTo("clerk");
  }

  @Test
  void amendmentEvolvesInstitutionsAndMovesProceduresInNewConstitution() {
    UUID polityId = UUID.randomUUID();
    UUID currentConstitutionId = UUID.randomUUID();
    UUID assemblyId = UUID.randomUUID();
    UUID councilId = UUID.randomUUID();
    UUID courtId = UUID.randomUUID();
    UUID jurisdictionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion current = constitution(polityId, currentConstitutionId, "Old body");
    Jurisdiction jurisdiction = jurisdiction(polityId, jurisdictionId);
    Motion motion = motion(polityId, actor.getId(), currentConstitutionId);
    ConstitutionAmendmentProposal proposal =
        new ConstitutionAmendmentProposal(
            polityId, motion.getId(), "Second Constitution", "New body");
    ReflectionTestUtils.setField(proposal, "id", UUID.randomUUID());
    Institution assembly =
        new Institution(
            polityId,
            jurisdictionId,
            currentConstitutionId,
            "Citizens' Assembly",
            InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(assembly, "id", assemblyId);
    Institution court =
        new Institution(
            polityId,
            jurisdictionId,
            currentConstitutionId,
            "Magistrates' Court",
            InstitutionKind.JUDICIARY);
    ReflectionTestUtils.setField(court, "id", courtId);
    Institution council =
        new Institution(
            polityId,
            jurisdictionId,
            currentConstitutionId,
            "Citizens' Council",
            InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(council, "id", councilId);
    Procedure procedure =
        new Procedure(
            polityId,
            currentConstitutionId,
            assemblyId,
            Procedure.ORDINARY_RESOLUTION,
            "Ordinary resolution",
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            0,
            24,
            EffectType.ADOPT_RESOLUTION);
    ConstitutionInstitutionChangeProposal retireAssembly =
        new ConstitutionInstitutionChangeProposal(
            polityId,
            proposal.getId(),
            ConstitutionInstitutionChangeAction.RETIRE,
            assemblyId,
            null,
            null,
            null);
    ConstitutionInstitutionChangeProposal reviseCourt =
        new ConstitutionInstitutionChangeProposal(
            polityId,
            proposal.getId(),
            ConstitutionInstitutionChangeAction.REVISE,
            courtId,
            null,
            "High Court",
            null);
    ConstitutionInstitutionChangeProposal createAuditChamber =
        new ConstitutionInstitutionChangeProposal(
            polityId,
            proposal.getId(),
            ConstitutionInstitutionChangeAction.CREATE,
            null,
            jurisdictionId,
            "Audit Chamber",
            InstitutionKind.ASSEMBLY);
    ConstitutionProcedureChangeProposal moveProcedure =
        new ConstitutionProcedureChangeProposal(
            polityId,
            proposal.getId(),
            Procedure.ORDINARY_RESOLUTION,
            councilId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    when(amendmentProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionAmendmentProposalProjection.class, proposal)));
    when(institutionChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(
            List.of(
                projection(ConstitutionInstitutionChangeProposalProjection.class, retireAssembly),
                projection(ConstitutionInstitutionChangeProposalProjection.class, reviseCourt),
                projection(
                    ConstitutionInstitutionChangeProposalProjection.class, createAuditChamber)));
    when(procedureChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(
            List.of(
                projection(ConstitutionProcedureChangeProposalProjection.class, moveProcedure)));
    when(officeChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(powerChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of());
    when(constitutions.findEntityById(currentConstitutionId)).thenReturn(Optional.of(current));
    when(constitutions.saveAndFlush(any(ConstitutionVersion.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(jurisdictions.findEntitiesByPolityId(polityId)).thenReturn(List.of(jurisdiction));
    when(institutions.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(assembly, court, council));
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(procedures.findEntitiesByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(procedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(currentConstitutionId))
        .thenReturn(List.of());
    when(powers.findEntitiesByConstitutionVersionId(currentConstitutionId)).thenReturn(List.of());

    effect.apply(motion, actor, current, NOW);

    ArgumentCaptor<ConstitutionVersion> constitutionCaptor =
        ArgumentCaptor.forClass(ConstitutionVersion.class);
    verify(constitutions, org.mockito.Mockito.times(2)).saveAndFlush(constitutionCaptor.capture());
    UUID amendedConstitutionId = constitutionCaptor.getAllValues().get(1).getId();
    ArgumentCaptor<Institution> institutionCaptor = ArgumentCaptor.forClass(Institution.class);
    verify(institutions, org.mockito.Mockito.times(3)).saveAndFlush(institutionCaptor.capture());
    assertThat(institutionCaptor.getAllValues())
        .extracting(Institution::getName)
        .containsExactlyInAnyOrder("High Court", "Citizens' Council", "Audit Chamber");
    assertThat(institutionCaptor.getAllValues())
        .allSatisfy(
            institution -> {
              assertThat(institution.getConstitutionVersionId()).isEqualTo(amendedConstitutionId);
              assertThat(institution.getJurisdictionId()).isEqualTo(jurisdictionId);
            });
    Institution copiedCouncil =
        institutionCaptor.getAllValues().stream()
            .filter(institution -> institution.getName().equals("Citizens' Council"))
            .findFirst()
            .orElseThrow();
    ArgumentCaptor<Procedure> procedureCaptor = ArgumentCaptor.forClass(Procedure.class);
    verify(procedures).save(procedureCaptor.capture());
    assertThat(procedureCaptor.getValue().getInstitutionId()).isEqualTo(copiedCouncil.getId());
  }

  private Motion motion(UUID polityId, UUID actorMembershipId, UUID constitutionId) {
    return motion(polityId, actorMembershipId, constitutionId, EffectType.AMEND_CONSTITUTION);
  }

  private Motion motion(
      UUID polityId, UUID actorMembershipId, UUID constitutionId, EffectType effectType) {
    Motion motion =
        new Motion(
            polityId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            constitutionId,
            UUID.randomUUID(),
            actorMembershipId,
            "Amend constitution",
            "New body",
            effectType,
            NOW.minusHours(1),
            NOW.minusHours(1),
            NOW.plusHours(1),
            NOW.plusHours(1));
    ReflectionTestUtils.setField(motion, "id", UUID.randomUUID());
    return motion;
  }

  private ConstitutionVersion constitution(UUID polityId, UUID constitutionId, String body) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", body, NOW.minusDays(1));
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    return constitution;
  }

  private Membership member(UUID polityId) {
    Membership member =
        new Membership(
            polityId,
            UUID.randomUUID(),
            "subject:" + UUID.randomUUID(),
            "friend@example.com",
            "Friend",
            NOW,
            null);
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private Jurisdiction jurisdiction(UUID polityId, UUID jurisdictionId) {
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", jurisdictionId);
    return jurisdiction;
  }

  private <T> T withId(T entity) {
    if (ReflectionTestUtils.getField(entity, "id") == null) {
      ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
    return entity;
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
