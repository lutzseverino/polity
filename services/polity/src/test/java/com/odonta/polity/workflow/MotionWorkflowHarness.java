package com.odonta.polity.workflow;

import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.effect.MotionEffectApplier;
import com.odonta.polity.effect.OfficialActVoidRemedy;
import com.odonta.polity.evaluator.ConstitutionAmendmentEvaluator;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.evaluator.VotingEvaluator;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalReviewProposalRepository;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeElectionBallotPreferenceRepository;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProposalRepository;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.repository.VoteRepository;
import com.odonta.polity.resolver.ActiveMembershipResolver;
import com.odonta.polity.resolver.ConstitutionAmendmentStateResolver;
import com.odonta.polity.resolver.MotionResultResolver;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.resolver.ProcedureElectorateResolver;
import com.odonta.polity.service.MembershipService;
import com.odonta.polity.service.OfficialRecordService;
import com.odonta.polity.service.PolityActionAvailabilityService;
import com.odonta.polity.service.PolityService;
import java.time.Clock;

/** Test-only composition root for the motion workflow boundaries. */
record MotionWorkflowHarness(
    IntroduceMotionWorkflow introduceMotion,
    IntroduceSanctionMotionWorkflow sanctionMotions,
    IntroduceOfficeElectionMotionWorkflow officeElectionMotions,
    IntroduceDisbandmentMotionWorkflow disbandmentMotions,
    IntroduceConstitutionAmendmentMotionWorkflow constitutionAmendmentMotions,
    IntroduceAppealMotionWorkflow appealMotions,
    IntroduceOfficeTermReviewMotionWorkflow officeTermReviewMotions,
    IntroduceConstitutionalReviewMotionWorkflow constitutionalReviewMotions,
    CastMotionVoteWorkflow castMotionVote,
    CastOfficeElectionBallotWorkflow castOfficeElectionBallot,
    RespondOfficeElectionCandidacyWorkflow respondOfficeElectionCandidacy,
    CertifyMotionWorkflow certifyMotion) {

  static MotionWorkflowHarness create(
      Clock clock,
      ActiveMembershipResolver activeMemberships,
      AppealRepository appeals,
      AppealProposalRepository appealProposals,
      CertificationRepository certifications,
      ConstitutionalAuthority authority,
      ConstitutionAmendmentEvaluator amendmentEvaluator,
      ConstitutionAmendmentProposalRepository amendmentProposals,
      ConstitutionAmendmentStateResolver amendmentStates,
      ConstitutionInstitutionChangeProposalRepository institutionChangeProposals,
      ConstitutionOfficeChangeProposalRepository officeChangeProposals,
      ConstitutionPowerChangeProposalRepository powerChangeProposals,
      ConstitutionProcedureChangeProposalRepository procedureChangeProposals,
      ConstitutionVersionRepository constitutions,
      ConstitutionalReviewProposalRepository constitutionalReviewProposals,
      ConstitutionalReviewRepository constitutionalReviews,
      MotionEffectApplier effects,
      InstitutionRepository institutions,
      JurisdictionRepository jurisdictions,
      OfficeTermReviewProposalRepository officeTermReviewProposals,
      MotionElectorRepository electors,
      MembershipService membershipService,
      MotionResultResolver results,
      MotionRepository motions,
      OfficeElectionBallotRepository officeElectionBallots,
      OfficeElectionBallotPreferenceRepository officeElectionBallotPreferences,
      OfficeElectionCandidateRepository officeElectionCandidates,
      OfficeElectionEvaluator officeElections,
      OfficeElectionProposalRepository officeElectionProposals,
      OfficeRepository offices,
      OfficeTermRepository officeTerms,
      OfficialActVoidRemedy officialActVoidRemedies,
      OfficialRecordRepository officialRecordEntries,
      OfficialRecordService officialRecords,
      PolityRepository polityRepository,
      PolityService polities,
      PolityActionAvailabilityService polityActionAvailability,
      ProcedureElectorateResolver procedureElectorates,
      ProcedureRepository procedures,
      SanctionProposalRepository sanctionProposals,
      SanctionRepository sanctions,
      VoteRepository votes) {
    MotionIntroducer introducer =
        new MotionIntroducer(
            clock,
            activeMemberships,
            institutions,
            electors,
            motions,
            officialRecords,
            new PolityContextResolver(constitutions, jurisdictions, polityRepository),
            polities,
            procedureElectorates,
            procedures,
            results);
    MotionCommandContext commandContext =
        new MotionCommandContext(clock, activeMemberships, motions, polities, results);

    return new MotionWorkflowHarness(
        new IntroduceMotionWorkflow(authority, introducer),
        new IntroduceSanctionMotionWorkflow(
            polityActionAvailability,
            activeMemberships,
            authority,
            introducer,
            procedureElectorates,
            procedures,
            sanctionProposals),
        new IntroduceOfficeElectionMotionWorkflow(
            authority,
            activeMemberships,
            membershipService,
            introducer,
            officeElectionCandidates,
            officeElectionProposals,
            offices,
            officeTerms),
        new IntroduceDisbandmentMotionWorkflow(authority, introducer, polities),
        new IntroduceConstitutionAmendmentMotionWorkflow(
            amendmentEvaluator,
            amendmentProposals,
            amendmentStates,
            authority,
            institutionChangeProposals,
            introducer,
            officeChangeProposals,
            powerChangeProposals,
            procedureChangeProposals),
        new IntroduceAppealMotionWorkflow(
            appeals, appealProposals, authority, introducer, motions, sanctions),
        new IntroduceOfficeTermReviewMotionWorkflow(
            authority,
            membershipService,
            introducer,
            motions,
            offices,
            officeTerms,
            officeTermReviewProposals),
        new IntroduceConstitutionalReviewMotionWorkflow(
            authority,
            constitutionalReviewProposals,
            constitutionalReviews,
            introducer,
            motions,
            officeTerms,
            officialActVoidRemedies,
            officialRecordEntries,
            sanctions),
        new CastMotionVoteWorkflow(commandContext, electors, officialRecords, votes),
        new CastOfficeElectionBallotWorkflow(
            officeElectionBallots,
            officeElectionBallotPreferences,
            officeElectionCandidates,
            commandContext,
            electors,
            officialRecords),
        new RespondOfficeElectionCandidacyWorkflow(
            officeElectionCandidates, commandContext, membershipService, officialRecords),
        new CertifyMotionWorkflow(
            appealProposals,
            certifications,
            authority,
            constitutions,
            commandContext,
            effects,
            electors,
            motions,
            new OfficeElectionCandidateEligibilityApplicator(
                activeMemberships, officeElectionCandidates, membershipService),
            new OfficeElectionTallyResolver(
                activeMemberships,
                officeElectionBallotPreferences,
                officeElectionCandidates,
                officeElections,
                membershipService,
                officeElectionProposals,
                offices,
                officeTerms),
            officialRecords,
            procedures,
            votes,
            new VotingEvaluator()));
  }
}
