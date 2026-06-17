package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.common.api.ApiException;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeAssignmentProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class EffectApplicationServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-14T10:00:00Z");

  private final AppealProposalRepository appealProposals = mock(AppealProposalRepository.class);
  private final AppealRepository appeals = mock(AppealRepository.class);
  private final ConstitutionAmendmentProposalRepository amendmentProposals =
      mock(ConstitutionAmendmentProposalRepository.class);
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals =
      mock(ConstitutionProcedureChangeProposalRepository.class);
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  private final InstitutionRepository institutions = mock(InstitutionRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final OfficeAssignmentProposalRepository officeAssignmentProposals =
      mock(OfficeAssignmentProposalRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final OfficialRecordWriter record = mock(OfficialRecordWriter.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final ResolutionRepository resolutions = mock(ResolutionRepository.class);
  private final SanctionProposalRepository sanctionProposals =
      mock(SanctionProposalRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);

  private final EffectApplicationService service =
      new EffectApplicationService(
          appealProposals,
          appeals,
          amendmentProposals,
          procedureChangeProposals,
          constitutions,
          powers,
          institutions,
          memberships,
          officeAssignmentProposals,
          offices,
          officeTerms,
          record,
          procedures,
          resolutions,
          sanctionProposals,
          sanctions);

  @Test
  void amendmentSupersedesCurrentConstitutionAndCopiesKernelRows() {
    UUID polityId = UUID.randomUUID();
    UUID currentConstitutionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    UUID procedureId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion current =
        constitution(polityId, currentConstitutionId, 1, "Starter Constitution", "Old body");
    Motion motion = motion(polityId, actor.getId(), currentConstitutionId);
    ConstitutionAmendmentProposal proposal =
        new ConstitutionAmendmentProposal(
            polityId,
            motion.getId(),
            "Second Constitution",
            "New body",
            "ordinary-resolution(threshold=TWO_THIRDS_CAST)");
    ReflectionTestUtils.setField(proposal, "id", UUID.randomUUID());
    ConstitutionProcedureChangeProposal procedureChange =
        new ConstitutionProcedureChangeProposal(
            polityId,
            proposal.getId(),
            Procedure.ORDINARY_RESOLUTION,
            null,
            null,
            VotingThreshold.TWO_THIRDS_CAST,
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
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            0,
            24,
            EffectType.ADOPT_RESOLUTION);
    ReflectionTestUtils.setField(procedure, "id", procedureId);

    when(amendmentProposals.findByMotionId(motion.getId())).thenReturn(Optional.of(proposal));
    when(procedureChangeProposals.findByAmendmentProposalId(proposal.getId()))
        .thenReturn(List.of(procedureChange));
    when(constitutions.findById(currentConstitutionId)).thenReturn(Optional.of(current));
    when(constitutions.saveAndFlush(any(ConstitutionVersion.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(institutions.findByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(institution));
    when(institutions.saveAndFlush(any(Institution.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(procedures.findByConstitutionVersionId(currentConstitutionId))
        .thenReturn(List.of(procedure));
    when(offices.findByConstitutionVersionIdOrderByName(currentConstitutionId))
        .thenReturn(List.of());
    when(powers.findByConstitutionVersionId(currentConstitutionId)).thenReturn(List.of());

    service.apply(motion, actor, current, NOW);

    assertThat(current.getStatus()).isEqualTo(ConstitutionStatus.SUPERSEDED);
    ArgumentCaptor<ConstitutionVersion> constitutionCaptor =
        ArgumentCaptor.forClass(ConstitutionVersion.class);
    verify(constitutions, org.mockito.Mockito.times(2)).saveAndFlush(constitutionCaptor.capture());
    assertThat(constitutionCaptor.getAllValues().get(1).getVersion()).isEqualTo(2);
    ArgumentCaptor<Procedure> procedureCaptor = ArgumentCaptor.forClass(Procedure.class);
    verify(procedures).save(procedureCaptor.capture());
    assertThat(procedureCaptor.getValue().getThreshold())
        .isEqualTo(VotingThreshold.TWO_THIRDS_CAST);
    verify(record)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.CONSTITUTION_AMENDED),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void appealEffectRejectsExpiredSanctionsAtCertificationTime() {
    UUID polityId = UUID.randomUUID();
    UUID sanctionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution =
        constitution(polityId, UUID.randomUUID(), 1, "Starter Constitution", "Body");
    Motion motion = motion(polityId, actor.getId(), constitution.getId(), EffectType.GRANT_APPEAL);
    Sanction sanction =
        new Sanction(
            polityId,
            UUID.randomUUID(),
            actor.getId(),
            SanctionType.WARNING,
            "Reason",
            NOW.minusDays(2),
            NOW.minusDays(1));
    ReflectionTestUtils.setField(sanction, "id", sanctionId);
    AppealProposal proposal =
        new AppealProposal(polityId, motion.getId(), sanctionId, actor.getId(), "Evidence");

    when(appealProposals.findByMotionId(motion.getId())).thenReturn(Optional.of(proposal));
    when(sanctions.findByIdAndPolityId(sanctionId, polityId)).thenReturn(Optional.of(sanction));

    assertThatThrownBy(() -> service.apply(motion, actor, constitution, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessage("Only active sanctions can be appealed.");
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

  private ConstitutionVersion constitution(
      UUID polityId, UUID constitutionId, int version, String title, String body) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, version, title, body, NOW.minusDays(1));
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    return constitution;
  }

  private Membership member(UUID polityId) {
    Membership member =
        new Membership(
            polityId, UUID.randomUUID(), "subject", "friend@example.com", "Friend", NOW, null);
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private <T> T withId(T entity) {
    if (ReflectionTestUtils.getField(entity, "id") == null) {
      ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
    return entity;
  }
}
