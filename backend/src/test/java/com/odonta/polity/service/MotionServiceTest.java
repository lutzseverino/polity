package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.MotionApplicationMapper;
import com.odonta.polity.model.CastVoteInput;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.CreateAppealMotionInput;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeAssignmentProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.repository.VoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class MotionServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-11T20:00:00Z");

  private final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  private final AppealRepository appeals = mock(AppealRepository.class);
  private final AppealProposalRepository appealProposals = mock(AppealProposalRepository.class);
  private final CertificationRepository certifications = mock(CertificationRepository.class);
  private final ConstitutionalAuthority authority = mock(ConstitutionalAuthority.class);
  private final ConstitutionAmendmentProposalRepository amendmentProposals =
      mock(ConstitutionAmendmentProposalRepository.class);
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals =
      mock(ConstitutionProcedureChangeProposalRepository.class);
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final EffectApplicationService effects = mock(EffectApplicationService.class);
  private final MotionElectorRepository electors = mock(MotionElectorRepository.class);
  private final MembershipReader membershipReader = mock(MembershipReader.class);
  private final MemberStandingService standing = mock(MemberStandingService.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final MotionRepository motions = mock(MotionRepository.class);
  private final OfficeAssignmentProposalRepository officeAssignmentProposals =
      mock(OfficeAssignmentProposalRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final OfficialRecordWriter record = mock(OfficialRecordWriter.class);
  private final PolityService polities = mock(PolityService.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final SanctionProposalRepository sanctionProposals =
      mock(SanctionProposalRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private final VoteRepository votes = mock(VoteRepository.class);
  private MotionService service;

  @BeforeEach
  void setUp() {
    service =
        new MotionService(
            Clock.fixed(Instant.from(NOW), ZoneOffset.UTC),
            access,
            appeals,
            appealProposals,
            certifications,
            authority,
            amendmentProposals,
            procedureChangeProposals,
            constitutions,
            effects,
            electors,
            membershipReader,
            standing,
            memberships,
            Mappers.getMapper(MotionApplicationMapper.class),
            motions,
            officeAssignmentProposals,
            offices,
            record,
            polities,
            procedures,
            sanctionProposals,
            sanctions,
            votes,
            new VotingEvaluator());
    when(standing.hasPoliticalStanding(any(Membership.class), any(OffsetDateTime.class)))
        .thenReturn(true);
  }

  @Test
  void rejectsVotesFromMembersOutsideTheFrozenElectorate() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership member = member(polityId, userId, UUID.randomUUID());
    Motion motion = motion(polityId, motionId, member.getId());
    when(membershipReader.active(polityId, userId)).thenReturn(member);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(electors.existsByMotionIdAndMembershipId(motionId, member.getId())).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.vote(
                    polityId,
                    motionId,
                    new AuthenticatedUser(userId, "subject", "Late Member"),
                    new CastVoteInput(VoteChoice.YES)))
        .isInstanceOf(ApiException.class)
        .hasMessage("This member was not eligible when voting opened.");
  }

  @Test
  void certificationUsesTheFrozenElectorateAndAdoptsAResolution() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    UUID requesterMembershipId = UUID.randomUUID();
    Membership requester = member(polityId, requesterUserId, requesterMembershipId);
    Motion motion = motion(polityId, motionId, requesterMembershipId);
    ReflectionTestUtils.setField(motion, "votingClosesAt", NOW.minusMinutes(1));
    ReflectionTestUtils.setField(motion, "certificationOpensAt", NOW.minusMinutes(1));
    ConstitutionVersion constitution = constitution(polityId, motion.getConstitutionVersionId());
    Procedure procedure = procedure(polityId, motion.getProcedureId(), constitution.getId());
    Vote first = vote(polityId, motionId, requesterMembershipId, VoteChoice.YES);
    Vote second = vote(polityId, motionId, UUID.randomUUID(), VoteChoice.YES);

    when(membershipReader.active(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findById(constitution.getId())).thenReturn(Optional.of(constitution));
    when(procedures.findById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(electors.countByMotionId(motionId)).thenReturn(3L);
    when(votes.findByMotionId(motionId)).thenReturn(List.of(first, second));
    when(certifications.saveAndFlush(any(Certification.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(memberships.findById(requesterMembershipId)).thenReturn(Optional.of(requester));
    when(certifications.findByMotionId(motionId))
        .thenAnswer(
            invocation -> {
              Certification certification =
                  new Certification(
                      polityId,
                      motionId,
                      requesterMembershipId,
                      new VotingEvaluator().evaluate(procedure, 3, List.of(first, second)),
                      NOW);
              return Optional.of(certification);
            });
    when(motions.findProjectedByIdAndPolityId(motionId, polityId))
        .thenAnswer(invocation -> Optional.of(projection(motion, procedure)));

    var result =
        service.certify(
            polityId, motionId, new AuthenticatedUser(requesterUserId, "subject", "Requester"));

    assertThat(result.status()).isEqualTo(MotionStatus.ENACTED);
    assertThat(result.tally().eligible()).isEqualTo(3);
    assertThat(result.tally().passed()).isTrue();
    verify(effects).apply(motion, requester, constitution, NOW);
    verify(record).append(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void rejectsCertificationBeforeTheFrozenCertificationWindow() {
    UUID polityId = UUID.randomUUID();
    UUID motionId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    UUID requesterMembershipId = UUID.randomUUID();
    Membership requester = member(polityId, requesterUserId, requesterMembershipId);
    Motion motion = motion(polityId, motionId, requesterMembershipId);
    ConstitutionVersion constitution = constitution(polityId, motion.getConstitutionVersionId());

    when(membershipReader.active(polityId, requesterUserId)).thenReturn(requester);
    when(motions.findByIdAndPolityId(motionId, polityId)).thenReturn(Optional.of(motion));
    when(constitutions.findById(constitution.getId())).thenReturn(Optional.of(constitution));

    assertThatThrownBy(
            () ->
                service.certify(
                    polityId,
                    motionId,
                    new AuthenticatedUser(requesterUserId, "subject", "Requester")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This motion cannot be certified until voting closes.");
  }

  @Test
  void createAppealUsesTheAppealProcedureEffectType() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    Sanction sanction = sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.plusDays(7));
    Motion[] saved = new Motion[1];

    when(membershipReader.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(procedure));
    when(sanctions.findByIdAndPolityId(sanction.getId(), polityId))
        .thenReturn(Optional.of(sanction));
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(memberships.findByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, com.odonta.polity.model.MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(certifications.findByMotionId(any(UUID.class))).thenReturn(Optional.empty());
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findByMotionId(any(UUID.class))).thenReturn(List.of());

    service.createAppeal(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateAppealMotionInput(sanction.getId(), "Fresh evidence"));

    ArgumentCaptor<Motion> motionCaptor = ArgumentCaptor.forClass(Motion.class);
    verify(motions).saveAndFlush(motionCaptor.capture());
    assertThat(motionCaptor.getValue().getEffectType()).isEqualTo(EffectType.GRANT_APPEAL);
  }

  @Test
  void rejectsAppealsForExpiredSanctions() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    Sanction sanction =
        sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.minusMinutes(1));
    stubAppealCreationContext(polityId, actorUserId, sanction);

    assertThatThrownBy(
            () ->
                service.createAppeal(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateAppealMotionInput(sanction.getId(), "Too late")))
        .isInstanceOf(ApiException.class)
        .hasMessage("Only active sanctions can be appealed.");
  }

  @Test
  void rejectsDuplicateGrantedAppeals() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    Sanction sanction = sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.plusDays(7));
    stubAppealCreationContext(polityId, actorUserId, sanction);
    when(appeals.existsByPolityIdAndSanctionId(polityId, sanction.getId())).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.createAppeal(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateAppealMotionInput(sanction.getId(), "Already handled")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This sanction has already been appealed.");
  }

  @Test
  void rejectsDuplicateOpenAppeals() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    Sanction sanction = sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.plusDays(7));
    stubAppealCreationContext(polityId, actorUserId, sanction);
    when(appealProposals.existsByPolityIdAndSanctionIdAndMotionStatus(
            polityId, sanction.getId(), MotionStatus.VOTING))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                service.createAppeal(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateAppealMotionInput(sanction.getId(), "Already open")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This sanction already has an open appeal motion.");
  }

  private Membership member(UUID polityId, UUID userId, UUID membershipId) {
    Membership member =
        new Membership(
            polityId, userId, "subject-" + userId, "friend@example.com", "Friend", NOW, null);
    ReflectionTestUtils.setField(member, "id", membershipId);
    return member;
  }

  private Motion motion(UUID polityId, UUID motionId, UUID introducedBy) {
    Motion motion =
        new Motion(
            polityId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            introducedBy,
            "Make Friday officially pizza night",
            "The polity recognizes Friday as pizza night.",
            EffectType.ADOPT_RESOLUTION,
            NOW.minusHours(1),
            NOW.minusHours(1),
            NOW.plusHours(1),
            NOW.plusHours(1));
    ReflectionTestUtils.setField(motion, "id", motionId);
    return motion;
  }

  private ConstitutionVersion constitution(UUID polityId, UUID constitutionId) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", "Body", NOW.minusDays(1));
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    return constitution;
  }

  private Procedure procedure(UUID polityId, UUID procedureId, UUID constitutionId) {
    return procedure(
        polityId,
        procedureId,
        constitutionId,
        Procedure.ORDINARY_RESOLUTION,
        "Ordinary resolution",
        EffectType.ADOPT_RESOLUTION);
  }

  private Procedure procedure(
      UUID polityId,
      UUID procedureId,
      UUID constitutionId,
      String code,
      String name,
      EffectType effectType) {
    Procedure procedure =
        new Procedure(
            polityId,
            constitutionId,
            UUID.randomUUID(),
            code,
            name,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            0,
            24,
            effectType);
    ReflectionTestUtils.setField(procedure, "id", procedureId);
    return procedure;
  }

  private Jurisdiction jurisdiction(UUID polityId) {
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    return jurisdiction;
  }

  private Institution institution(UUID polityId, UUID jurisdictionId, UUID constitutionId) {
    Institution institution =
        new Institution(
            polityId, jurisdictionId, constitutionId, "Assembly", InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(institution, "id", UUID.randomUUID());
    return institution;
  }

  private Sanction sanction(
      UUID polityId, UUID motionId, UUID targetMembershipId, OffsetDateTime endsAt) {
    Sanction sanction =
        new Sanction(
            polityId,
            motionId,
            targetMembershipId,
            SanctionType.WARNING,
            "Reason",
            NOW.minusDays(1),
            endsAt);
    ReflectionTestUtils.setField(sanction, "id", UUID.randomUUID());
    return sanction;
  }

  private void stubAppealCreationContext(UUID polityId, UUID actorUserId, Sanction sanction) {
    Membership actor = member(polityId, actorUserId, UUID.randomUUID());
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    when(membershipReader.active(polityId, actorUserId)).thenReturn(actor);
    when(polities.constitution(polityId)).thenReturn(constitution);
    when(polities.jurisdiction(polityId)).thenReturn(jurisdiction);
    when(polities.institution(polityId, constitution)).thenReturn(institution);
    when(procedures.findByConstitutionVersionIdAndCode(constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(procedure));
    when(sanctions.findByIdAndPolityId(sanction.getId(), polityId))
        .thenReturn(Optional.of(sanction));
  }

  private Vote vote(UUID polityId, UUID motionId, UUID membershipId, VoteChoice choice) {
    return new Vote(polityId, motionId, membershipId, choice, NOW.minusMinutes(10));
  }

  private MotionProjection projection(Motion motion, Procedure procedure) {
    return new MotionProjection() {
      @Override
      public UUID getId() {
        return motion.getId();
      }

      @Override
      public String getTitle() {
        return motion.getTitle();
      }

      @Override
      public String getBody() {
        return motion.getBody();
      }

      @Override
      public MotionStatus getStatus() {
        return motion.getStatus();
      }

      @Override
      public EffectType getEffectType() {
        return motion.getEffectType();
      }

      @Override
      public int getConstitutionVersion() {
        return 1;
      }

      @Override
      public String getProcedureName() {
        return procedure.getName();
      }

      @Override
      public String getIntroducedByName() {
        return "Friend";
      }

      @Override
      public OffsetDateTime getOpenedAt() {
        return motion.getOpenedAt();
      }

      @Override
      public OffsetDateTime getVotingOpensAt() {
        return motion.getVotingOpensAt();
      }

      @Override
      public OffsetDateTime getVotingClosesAt() {
        return motion.getVotingClosesAt();
      }

      @Override
      public OffsetDateTime getCertificationOpensAt() {
        return motion.getCertificationOpensAt();
      }

      @Override
      public int getQuorumNumerator() {
        return procedure.getQuorumNumerator();
      }

      @Override
      public int getQuorumDenominator() {
        return procedure.getQuorumDenominator();
      }

      @Override
      public VotingThreshold getThreshold() {
        return procedure.getThreshold();
      }
    };
  }

  private <T> T withId(T entity) {
    if (ReflectionTestUtils.getField(entity, "id") == null) {
      ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
    return entity;
  }
}
