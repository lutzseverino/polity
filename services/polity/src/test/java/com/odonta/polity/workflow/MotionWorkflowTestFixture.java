package com.odonta.polity.workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.effect.MotionEffectApplier;
import com.odonta.polity.effect.OfficialActVoidRemedy;
import com.odonta.polity.evaluator.ConstitutionAmendmentEvaluator;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.evaluator.VotingEvaluator;
import com.odonta.polity.mapper.CertificationApplicationMapper;
import com.odonta.polity.mapper.ConstitutionAmendmentApplicationMapper;
import com.odonta.polity.mapper.MotionApplicationMapper;
import com.odonta.polity.mapper.OfficeElectionApplicationMapper;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionElector;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionBallotPreference;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.PolityStatus;
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
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionProjection;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.ConstitutionalReviewProposalRepository;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorProjection;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeElectionBallotPreferenceRepository;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProposalRepository;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureProjection;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.repository.VoteProjection;
import com.odonta.polity.repository.VoteRepository;
import com.odonta.polity.resolver.ActiveMembershipResolver;
import com.odonta.polity.resolver.ConstitutionAmendmentResultResolver;
import com.odonta.polity.resolver.ConstitutionAmendmentStateResolver;
import com.odonta.polity.resolver.MotionActionAvailabilityResolver;
import com.odonta.polity.resolver.MotionResultResolver;
import com.odonta.polity.resolver.OfficeElectionResultResolver;
import com.odonta.polity.resolver.ProcedureElectorateResolver;
import com.odonta.polity.service.MembershipService;
import com.odonta.polity.service.MotionService;
import com.odonta.polity.service.OfficialRecordService;
import com.odonta.polity.service.PolityActionAvailabilityService;
import com.odonta.polity.service.PolityService;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

abstract class MotionWorkflowTestFixture {
  static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-11T20:00:00Z");

  final PolityAccessPolicy access = mock(PolityAccessPolicy.class);
  final AppealRepository appeals = mock(AppealRepository.class);
  final AppealProposalRepository appealProposals = mock(AppealProposalRepository.class);
  final CertificationRepository certifications = mock(CertificationRepository.class);
  final Map<UUID, Certification> storedCertifications = new HashMap<>();
  final ConstitutionalAuthority authority = mock(ConstitutionalAuthority.class);
  final ConstitutionAmendmentEvaluator amendmentEvaluator = new ConstitutionAmendmentEvaluator();
  final ConstitutionAmendmentProposalRepository amendmentProposals =
      mock(ConstitutionAmendmentProposalRepository.class);
  final ConstitutionInstitutionChangeProposalRepository institutionChangeProposals =
      mock(ConstitutionInstitutionChangeProposalRepository.class);
  final ConstitutionOfficeChangeProposalRepository officeChangeProposals =
      mock(ConstitutionOfficeChangeProposalRepository.class);
  final ConstitutionPowerChangeProposalRepository powerChangeProposals =
      mock(ConstitutionPowerChangeProposalRepository.class);
  final ConstitutionProcedureChangeProposalRepository procedureChangeProposals =
      mock(ConstitutionProcedureChangeProposalRepository.class);
  final ConstitutionVersionRepository constitutions = mock(ConstitutionVersionRepository.class);
  final ConstitutionalPowerRepository powers = mock(ConstitutionalPowerRepository.class);
  final ConstitutionalReviewProposalRepository constitutionalReviewProposals =
      mock(ConstitutionalReviewProposalRepository.class);
  final ConstitutionalReviewRepository constitutionalReviews =
      mock(ConstitutionalReviewRepository.class);
  final MotionEffectApplier effects = mock(MotionEffectApplier.class);
  final InstitutionRepository institutions = mock(InstitutionRepository.class);
  final JurisdictionRepository jurisdictions = mock(JurisdictionRepository.class);
  final OfficeTermReviewProposalRepository officeTermReviewProposals =
      mock(OfficeTermReviewProposalRepository.class);
  final MotionElectorRepository electors = mock(MotionElectorRepository.class);
  final MembershipService membershipService = mock(MembershipService.class);
  final MembershipRepository memberships = mock(MembershipRepository.class);
  final MotionRepository motions = mock(MotionRepository.class);
  final PolityRepository polityRepository = mock(PolityRepository.class);
  final OfficeElectionBallotRepository officeElectionBallots =
      mock(OfficeElectionBallotRepository.class);
  final OfficeElectionBallotPreferenceRepository officeElectionBallotPreferences =
      mock(OfficeElectionBallotPreferenceRepository.class);
  final OfficeElectionCandidateRepository officeElectionCandidates =
      mock(OfficeElectionCandidateRepository.class);
  final OfficeElectionEvaluator officeElections = new OfficeElectionEvaluator();
  final OfficeElectionProposalRepository officeElectionProposals =
      mock(OfficeElectionProposalRepository.class);
  final OfficeRepository offices = mock(OfficeRepository.class);
  final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  final ResolutionRepository resolutions = mock(ResolutionRepository.class);
  final OfficialRecordRepository officialRecordEntries = mock(OfficialRecordRepository.class);
  final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  final PolityService polities = mock(PolityService.class);
  final PolityActionAvailabilityService polityActionAvailability =
      mock(PolityActionAvailabilityService.class);
  final ProcedureElectorateResolver procedureElectorates = mock(ProcedureElectorateResolver.class);
  final ProcedureRepository procedures = mock(ProcedureRepository.class);
  final SanctionProposalRepository sanctionProposals = mock(SanctionProposalRepository.class);
  final SanctionRepository sanctions = mock(SanctionRepository.class);
  final OfficialActVoidRemedy officialActVoidRemedies =
      new OfficialActVoidRemedy(officeTerms, resolutions, sanctions);
  final VoteRepository votes = mock(VoteRepository.class);
  final ConstitutionAmendmentStateResolver amendmentStates =
      new ConstitutionAmendmentStateResolver(
          powers, institutions, jurisdictions, offices, officeTerms, procedures);
  final ActiveMembershipResolver activeMemberships = mock(ActiveMembershipResolver.class);
  MotionService queryService;
  IntroduceMotionWorkflow introduceMotion;
  IntroduceSanctionMotionWorkflow sanctionMotions;
  IntroduceOfficeElectionMotionWorkflow officeElectionMotions;
  IntroduceDisbandmentMotionWorkflow disbandmentMotions;
  IntroduceConstitutionAmendmentMotionWorkflow constitutionAmendmentMotions;
  IntroduceAppealMotionWorkflow appealMotions;
  IntroduceOfficeTermReviewMotionWorkflow officeTermReviewMotions;
  IntroduceConstitutionalReviewMotionWorkflow constitutionalReviewMotions;
  CastMotionVoteWorkflow castMotionVote;
  CastOfficeElectionBallotWorkflow castOfficeElectionBallot;
  RespondOfficeElectionCandidacyWorkflow respondOfficeElectionCandidacy;
  CertifyMotionWorkflow certifyMotion;

  @BeforeEach
  void setUp() {
    MotionApplicationMapper motionMapper = Mappers.getMapper(MotionApplicationMapper.class);
    CertificationApplicationMapper certificationMapper =
        Mappers.getMapper(CertificationApplicationMapper.class);
    OfficeElectionApplicationMapper officeElectionMapper =
        Mappers.getMapper(OfficeElectionApplicationMapper.class);
    ConstitutionAmendmentApplicationMapper amendmentMapper =
        Mappers.getMapper(ConstitutionAmendmentApplicationMapper.class);
    MotionActionAvailabilityResolver actionAvailability =
        new MotionActionAvailabilityResolver(
            Clock.fixed(Instant.from(NOW), ZoneOffset.UTC),
            appealProposals,
            authority,
            powers,
            membershipService,
            officeElectionCandidates,
            officeTerms);
    MotionResultResolver results =
        new MotionResultResolver(
            Clock.fixed(Instant.from(NOW), ZoneOffset.UTC),
            motionMapper,
            certificationMapper,
            motions,
            polityRepository,
            constitutions,
            procedures,
            memberships,
            certifications,
            electors,
            votes,
            new VotingEvaluator(),
            new OfficeElectionResultResolver(
                officeElectionMapper,
                officeElections,
                officeElectionProposals,
                officeElectionCandidates,
                officeElectionBallots,
                officeElectionBallotPreferences,
                offices,
                officeTerms,
                memberships,
                membershipService),
            new ConstitutionAmendmentResultResolver(
                amendmentMapper,
                amendmentProposals,
                institutionChangeProposals,
                procedureChangeProposals,
                officeChangeProposals,
                powerChangeProposals),
            actionAvailability);
    Clock clock = Clock.fixed(Instant.from(NOW), ZoneOffset.UTC);
    MotionWorkflowHarness workflows =
        MotionWorkflowHarness.create(
            clock,
            activeMemberships,
            appeals,
            appealProposals,
            certifications,
            authority,
            amendmentEvaluator,
            amendmentProposals,
            amendmentStates,
            institutionChangeProposals,
            officeChangeProposals,
            powerChangeProposals,
            procedureChangeProposals,
            constitutions,
            constitutionalReviewProposals,
            constitutionalReviews,
            effects,
            institutions,
            jurisdictions,
            officeTermReviewProposals,
            electors,
            membershipService,
            results,
            motions,
            officeElectionBallots,
            officeElectionBallotPreferences,
            officeElectionCandidates,
            officeElections,
            officeElectionProposals,
            offices,
            officeTerms,
            officialActVoidRemedies,
            officialRecordEntries,
            officialRecords,
            polityRepository,
            polities,
            polityActionAvailability,
            procedureElectorates,
            procedures,
            sanctionProposals,
            sanctions,
            votes);
    queryService = new MotionService(access, memberships, motions, results);
    introduceMotion = workflows.introduceMotion();
    sanctionMotions = workflows.sanctionMotions();
    officeElectionMotions = workflows.officeElectionMotions();
    disbandmentMotions = workflows.disbandmentMotions();
    constitutionAmendmentMotions = workflows.constitutionAmendmentMotions();
    appealMotions = workflows.appealMotions();
    officeTermReviewMotions = workflows.officeTermReviewMotions();
    constitutionalReviewMotions = workflows.constitutionalReviewMotions();
    castMotionVote = workflows.castMotionVote();
    castOfficeElectionBallot = workflows.castOfficeElectionBallot();
    respondOfficeElectionCandidacy = workflows.respondOfficeElectionCandidacy();
    certifyMotion = workflows.certifyMotion();
    when(activeMemberships.resolveById(any(UUID.class), any(UUID.class)))
        .thenAnswer(
            invocation -> memberships.findEntityById(invocation.getArgument(1)).orElseThrow());
    when(jurisdictions.findEntityByPolityIdAndKind(any(UUID.class), eq(JurisdictionKind.ROOT)))
        .thenAnswer(invocation -> Optional.of(jurisdiction(invocation.getArgument(0))));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), any(UUID.class)))
        .thenAnswer(
            invocation -> {
              UUID institutionId = invocation.getArgument(0);
              UUID polityId = invocation.getArgument(1);
              Institution fallback = institution(polityId, UUID.randomUUID(), UUID.randomUUID());
              ReflectionTestUtils.setField(fallback, "id", institutionId);
              return Optional.of(fallback);
            });
    when(procedures.findEntityByConstitutionVersionIdAndCode(any(UUID.class), any(String.class)))
        .thenAnswer(
            invocation ->
                Optional.of(
                    procedure(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        "Test procedure",
                        EffectType.ADOPT_RESOLUTION)));
    when(membershipService.hasPoliticalStanding(any(UUID.class), any(OffsetDateTime.class)))
        .thenReturn(true);
    PolityProjection polityProjection = mock(PolityProjection.class);
    when(polityProjection.getStatus()).thenReturn(PolityStatus.ACTIVE);
    when(polityRepository.findProjectedById(any(UUID.class)))
        .thenReturn(Optional.of(polityProjection));
    when(membershipService.politicalStanding(any(UUID.class), any(), any(OffsetDateTime.class)))
        .thenAnswer(invocation -> Set.copyOf(invocation.getArgument(1)));
    when(memberships.countByPolityIdAndStatus(any(UUID.class), eq(MembershipStatus.ACTIVE)))
        .thenReturn(1L);
    when(memberships.findEntityById(any(UUID.class)))
        .thenAnswer(
            invocation ->
                Optional.of(
                    member(UUID.randomUUID(), UUID.randomUUID(), invocation.getArgument(0))));
    when(memberships.findProjectionsByPolityIdAndIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .map(id -> memberships.findEntityById(id).orElseThrow())
                        .map(member -> projection(MembershipProjection.class, member))
                        .toList());
    when(constitutions.findProjectionsByPolityIdAndIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .map(id -> constitutions.findEntityById(id).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .map(
                            constitution ->
                                projection(ConstitutionVersionProjection.class, constitution))
                        .toList());
    when(procedures.findProjectionsByPolityIdAndIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .map(id -> procedures.findEntityById(id).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .map(procedure -> projection(ProcedureProjection.class, procedure))
                        .toList());
    when(votes.findProjectionsByPolityIdAndMotionIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .flatMap(id -> votes.findEntitiesByMotionId(id).stream())
                        .map(vote -> projection(VoteProjection.class, vote))
                        .toList());
    when(electors.findProjectionsByPolityIdAndMotionIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .flatMap(
                            id -> {
                              List<MotionElector> stored = electors.findEntitiesByMotionId(id);
                              if (!stored.isEmpty()) {
                                return stored.stream();
                              }
                              return java.util.stream.IntStream.range(
                                      0, Math.toIntExact(electors.countByMotionId(id)))
                                  .mapToObj(
                                      ignored ->
                                          new MotionElector(
                                              invocation.getArgument(0), id, UUID.randomUUID()));
                            })
                        .map(elector -> projection(MotionElectorProjection.class, elector))
                        .toList());
    when(certifications.findProjectionsByPolityIdAndMotionIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .map(storedCertifications::get)
                        .filter(java.util.Objects::nonNull)
                        .map(
                            certification ->
                                projection(
                                    com.odonta.polity.repository.CertificationProjection.class,
                                    certification))
                        .toList());
    when(officeElectionProposals.findProjectionsByPolityIdAndMotionIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .map(id -> officeElectionProposals.findProjectedByMotionId(id).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .toList());
    when(officeElectionCandidates.findProjectionsByPolityIdAndMotionIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .flatMap(id -> officeElectionCandidates.findEntitiesByMotionId(id).stream())
                        .map(
                            candidate ->
                                projection(
                                    com.odonta.polity.repository.OfficeElectionCandidateProjection
                                        .class,
                                    candidate))
                        .toList());
    when(officeElectionBallots.findProjectionsByPolityIdAndMotionIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .flatMap(id -> officeElectionBallots.findEntitiesByMotionId(id).stream())
                        .map(
                            ballot ->
                                projection(
                                    com.odonta.polity.repository.OfficeElectionBallotProjection
                                        .class,
                                    ballot))
                        .toList());
    when(officeElectionBallotPreferences
            .findProjectionsByPolityIdAndMotionIdInOrderByMembershipIdAscRankAsc(
                any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .flatMap(
                            id ->
                                officeElectionBallotPreferences
                                    .findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(id)
                                    .stream())
                        .map(
                            preference ->
                                projection(
                                    com.odonta.polity.repository
                                        .OfficeElectionBallotPreferenceProjection.class,
                                    preference))
                        .toList());
    when(offices.findProjectionsByPolityIdAndIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .map(
                            id ->
                                offices
                                    .findEntityByIdAndPolityId(id, invocation.getArgument(0))
                                    .orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .map(
                            office ->
                                projection(
                                    com.odonta.polity.repository.OfficeProjection.class, office))
                        .toList());
    when(amendmentProposals.findProjectionsByPolityIdAndMotionIdIn(any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .map(id -> amendmentProposals.findProjectedByMotionId(id).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .toList());
    when(institutionChangeProposals.findProjectionsByPolityIdAndAmendmentProposalIdIn(
            any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .flatMap(
                            id ->
                                institutionChangeProposals
                                    .findProjectionsByAmendmentProposalId(id)
                                    .stream())
                        .toList());
    when(procedureChangeProposals.findProjectionsByPolityIdAndAmendmentProposalIdIn(
            any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .flatMap(
                            id ->
                                procedureChangeProposals
                                    .findProjectionsByAmendmentProposalId(id)
                                    .stream())
                        .toList());
    when(officeChangeProposals.findProjectionsByPolityIdAndAmendmentProposalIdIn(
            any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .flatMap(
                            id ->
                                officeChangeProposals
                                    .findProjectionsByAmendmentProposalId(id)
                                    .stream())
                        .toList());
    when(powerChangeProposals.findProjectionsByPolityIdAndAmendmentProposalIdIn(
            any(UUID.class), any()))
        .thenAnswer(
            invocation ->
                ((java.util.Collection<UUID>) invocation.getArgument(1))
                    .stream()
                        .flatMap(
                            id ->
                                powerChangeProposals
                                    .findProjectionsByAmendmentProposalId(id)
                                    .stream())
                        .toList());
    when(procedureElectorates.electors(any(Procedure.class), any(OffsetDateTime.class)))
        .thenReturn(List.of(member(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())));
  }

  Membership member(UUID polityId, UUID userId, UUID membershipId) {
    Membership member =
        new Membership(
            polityId, userId, "subject-" + userId, "friend@example.com", "Friend", NOW, null);
    ReflectionTestUtils.setField(member, "id", membershipId);
    return member;
  }

  Motion motion(UUID polityId, UUID motionId, UUID introducedBy) {
    return motion(polityId, motionId, introducedBy, EffectType.ADOPT_RESOLUTION);
  }

  Motion motion(UUID polityId, UUID motionId, UUID introducedBy, EffectType effectType) {
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
            effectType,
            NOW.minusHours(1),
            NOW.minusHours(1),
            NOW.plusHours(1),
            NOW.plusHours(1));
    ReflectionTestUtils.setField(motion, "id", motionId);
    return motion;
  }

  ConstitutionVersion constitution(UUID polityId, UUID constitutionId) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", "Body", NOW.minusDays(1));
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    return constitution;
  }

  Procedure procedure(UUID polityId, UUID procedureId, UUID constitutionId) {
    return procedure(
        polityId,
        procedureId,
        constitutionId,
        Procedure.ORDINARY_RESOLUTION,
        "Ordinary resolution",
        EffectType.ADOPT_RESOLUTION);
  }

  Procedure procedure(
      UUID polityId,
      UUID procedureId,
      UUID constitutionId,
      UUID institutionId,
      String code,
      String name,
      EffectType effectType) {
    return procedure(
        polityId,
        procedureId,
        constitutionId,
        institutionId,
        code,
        name,
        effectType,
        VotingThreshold.SIMPLE_MAJORITY_CAST);
  }

  Procedure procedure(
      UUID polityId,
      UUID procedureId,
      UUID constitutionId,
      String code,
      String name,
      EffectType effectType) {
    return procedure(
        polityId,
        procedureId,
        constitutionId,
        UUID.randomUUID(),
        code,
        name,
        effectType,
        VotingThreshold.SIMPLE_MAJORITY_CAST);
  }

  Procedure procedure(
      UUID polityId,
      UUID procedureId,
      UUID constitutionId,
      UUID institutionId,
      String code,
      String name,
      EffectType effectType,
      VotingThreshold threshold) {
    Procedure procedure =
        new Procedure(
            polityId,
            constitutionId,
            institutionId,
            code,
            name,
            1,
            2,
            threshold,
            0,
            24,
            effectType);
    ReflectionTestUtils.setField(procedure, "id", procedureId);
    return procedure;
  }

  Procedure procedure(
      UUID polityId,
      UUID procedureId,
      UUID constitutionId,
      String code,
      String name,
      EffectType effectType,
      VotingThreshold threshold) {
    return procedure(
        polityId,
        procedureId,
        constitutionId,
        UUID.randomUUID(),
        code,
        name,
        effectType,
        threshold);
  }

  Jurisdiction jurisdiction(UUID polityId) {
    Jurisdiction jurisdiction = new Jurisdiction(polityId, "Commons", JurisdictionKind.ROOT);
    ReflectionTestUtils.setField(jurisdiction, "id", UUID.randomUUID());
    return jurisdiction;
  }

  Institution institution(UUID polityId, UUID jurisdictionId, UUID constitutionId) {
    Institution institution =
        new Institution(
            polityId, jurisdictionId, constitutionId, "Assembly", InstitutionKind.ASSEMBLY);
    ReflectionTestUtils.setField(institution, "id", UUID.randomUUID());
    return institution;
  }

  Office stewardOffice(UUID polityId, UUID constitutionId, UUID jurisdictionId) {
    Office office =
        new Office(
            polityId,
            constitutionId,
            jurisdictionId,
            Office.STEWARD,
            "Steward",
            "Coordinates.",
            14);
    ReflectionTestUtils.setField(office, "id", UUID.randomUUID());
    return office;
  }

  Sanction sanction(UUID polityId, UUID motionId, UUID targetMembershipId, OffsetDateTime endsAt) {
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

  void stubSanctionMotion(UUID polityId, Sanction sanction, UUID introducedBy) {
    when(motions.findEntityByIdAndPolityId(sanction.getMotionId(), polityId))
        .thenReturn(
            Optional.of(
                motion(polityId, sanction.getMotionId(), introducedBy, EffectType.APPLY_SANCTION)));
  }

  OfficeTerm officeTerm(
      UUID polityId, UUID officeId, String officeCode, UUID membershipId, OffsetDateTime endsAt) {
    OfficeTerm term =
        new OfficeTerm(polityId, officeId, officeCode, membershipId, NOW.minusDays(1), endsAt);
    ReflectionTestUtils.setField(term, "id", UUID.randomUUID());
    return term;
  }

  void stubAppealCreationContext(UUID polityId, UUID actorUserId, Sanction sanction) {
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
    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(procedure));
    when(sanctions.findEntityByIdAndPolityId(sanction.getId(), polityId))
        .thenReturn(Optional.of(sanction));
    stubSanctionMotion(polityId, sanction, UUID.randomUUID());
  }

  Vote yesVote(UUID polityId, UUID motionId, UUID membershipId) {
    return new Vote(polityId, motionId, membershipId, VoteChoice.YES, NOW.minusMinutes(10));
  }

  MotionProjection projection(Motion motion, Procedure procedure) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(
            motion.getPolityId(), 1, "Starter Constitution", "Body", NOW.minusDays(3));
    ReflectionTestUtils.setField(constitution, "id", motion.getConstitutionVersionId());
    when(constitutions.findEntityById(motion.getConstitutionVersionId()))
        .thenReturn(Optional.of(constitution));
    when(procedures.findEntityById(procedure.getId())).thenReturn(Optional.of(procedure));
    when(membershipService.displayName(motion.getIntroducedBy())).thenReturn("Friend");
    return new MotionProjection() {
      @Override
      public UUID getId() {
        return motion.getId();
      }

      @Override
      public UUID getConstitutionVersionId() {
        return motion.getConstitutionVersionId();
      }

      @Override
      public UUID getProcedureId() {
        return motion.getProcedureId();
      }

      @Override
      public UUID getIntroducedBy() {
        return motion.getIntroducedBy();
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
      public String getTitleKey() {
        return motion.getTitleKey();
      }

      @Override
      public String getBodyKey() {
        return motion.getBodyKey();
      }

      @Override
      public Map<String, Object> getTemplateParams() {
        return motion.getTemplateParams();
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
    };
  }

  <T> T withId(T entity) {
    if (ReflectionTestUtils.getField(entity, "id") == null) {
      ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
    return entity;
  }

  Certification saveCertification(Certification certification) {
    Certification saved = withId(certification);
    storedCertifications.put(saved.getMotionId(), saved);
    return saved;
  }

  OfficeElectionBallotPreference preference(
      UUID polityId, UUID motionId, UUID voterId, UUID candidateId, int rank) {
    return new OfficeElectionBallotPreference(
        polityId, motionId, UUID.randomUUID(), voterId, candidateId, rank);
  }

  static <T> T projection(Class<T> type, Object source) {
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

  long count(Iterable<?> values) {
    return StreamSupport.stream(values.spliterator(), false).count();
  }
}
