package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.polity.api.model.CastVoteInput;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Resolution;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.ResolutionRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

class MotionServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-11T20:00:00Z");

  private final CertificationRepository certifications = mock(CertificationRepository.class);
  private final ConstitutionalAuthority authority = mock(ConstitutionalAuthority.class);
  private final ConstitutionVersionRepository constitutions =
      mock(ConstitutionVersionRepository.class);
  private final MotionElectorRepository electors = mock(MotionElectorRepository.class);
  private final MembershipReader membershipReader = mock(MembershipReader.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final MotionRepository motions = mock(MotionRepository.class);
  private final OfficialRecordWriter record = mock(OfficialRecordWriter.class);
  private final PolityService polities = mock(PolityService.class);
  private final ProcedureRepository procedures = mock(ProcedureRepository.class);
  private final ResolutionRepository resolutions = mock(ResolutionRepository.class);
  private final VoteRepository votes = mock(VoteRepository.class);
  private MotionService service;

  @BeforeEach
  void setUp() {
    service =
        new MotionService(
            Clock.fixed(Instant.from(NOW), ZoneOffset.UTC),
            certifications,
            authority,
            constitutions,
            electors,
            membershipReader,
            memberships,
            motions,
            record,
            polities,
            procedures,
            resolutions,
            votes,
            new VotingEvaluator());
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
    when(resolutions.saveAndFlush(any(Resolution.class)))
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

    var result =
        service.certify(
            polityId, motionId, new AuthenticatedUser(requesterUserId, "subject", "Requester"));

    assertThat(result.motion().getStatus()).isEqualTo(MotionStatus.ENACTED);
    assertThat(result.tally().eligible()).isEqualTo(3);
    assertThat(result.tally().passed()).isTrue();
    verify(resolutions).saveAndFlush(any(Resolution.class));
    verify(record, org.mockito.Mockito.times(2))
        .append(any(), any(), any(), any(), any(), any(), any(), any(), any());
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
            NOW.minusHours(1));
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
    Procedure procedure =
        new Procedure(
            polityId,
            constitutionId,
            UUID.randomUUID(),
            Procedure.ORDINARY_RESOLUTION,
            "Ordinary resolution",
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY,
            EffectType.ADOPT_RESOLUTION);
    ReflectionTestUtils.setField(procedure, "id", procedureId);
    return procedure;
  }

  private Vote vote(UUID polityId, UUID motionId, UUID membershipId, VoteChoice choice) {
    return new Vote(polityId, motionId, membershipId, choice, NOW.minusMinutes(10));
  }
}
