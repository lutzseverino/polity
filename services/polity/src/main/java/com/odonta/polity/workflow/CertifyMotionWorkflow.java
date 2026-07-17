package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.effect.MotionEffectApplier;
import com.odonta.polity.evaluator.VotingEvaluator;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.VoteRepository;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.service.OfficialRecordService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CertifyMotionWorkflow {
  private final AppealProposalRepository appealProposals;
  private final CertificationRepository certifications;
  private final ConstitutionalAuthority authority;
  private final ConstitutionVersionRepository constitutions;
  private final MotionCommandContext context;
  private final MotionEffectApplier effects;
  private final MotionElectorRepository electors;
  private final MotionRepository motions;
  private final OfficeElectionCandidateEligibilityApplicator candidateEligibility;
  private final OfficeElectionTallyResolver electionTally;
  private final OfficialRecordService officialRecords;
  private final ProcedureRepository procedures;
  private final VoteRepository votes;
  private final VotingEvaluator voting;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult certify(UUID polityId, UUID motionId, AuthenticatedUser actor) {
    OffsetDateTime now = context.now();
    Membership requester = context.activeMember(polityId, actor);
    Motion motion = context.motion(polityId, motionId);
    context.requireVotingStatus(motion);
    if (now.isBefore(motion.getCertificationOpensAt())) {
      throw ApiException.conflict(
          "certification_not_open", "This motion cannot be certified until voting closes.");
    }
    ConstitutionVersion constitution =
        constitutions
            .findEntityById(motion.getConstitutionVersionId())
            .orElseThrow(PolityResource.CONSTITUTION::notFound);
    if (constitution.getStatus() != ConstitutionStatus.RATIFIED) {
      throw ApiException.conflict(
          "constitution_superseded",
          "This motion was introduced under a constitution that is no longer ratified.");
    }
    if (canCertifyOwnAppealWithoutStanding(motion, requester)) {
      authority.requireAppealCertification(requester, constitution);
    } else {
      authority.require(requester, constitution, PowerCode.REQUEST_CERTIFICATION);
    }
    Procedure procedure =
        procedures
            .findEntityById(motion.getProcedureId())
            .orElseThrow(PolityResource.PROCEDURE::notFound);
    int eligible = Math.toIntExact(electors.countByMotionId(motionId));
    boolean passed;
    Certification certification;
    if (motion.getEffectType() == EffectType.ELECT_OFFICE) {
      candidateEligibility.disqualifyWithoutStanding(motionId, now);
      OfficeElectionTallyResult electionOutcome =
          electionTally.resolve(procedure, eligible, motionId, now);
      passed = electionOutcome.passed();
      certification =
          certifications.saveAndFlush(
              new Certification(polityId, motionId, requester.getId(), electionOutcome, now));
    } else {
      VotingResult outcome =
          voting.evaluate(procedure, eligible, votes.findEntitiesByMotionId(motionId));
      passed = outcome.passed();
      certification =
          certifications.saveAndFlush(
              new Certification(polityId, motionId, requester.getId(), outcome, now));
    }
    motion.certify(passed, now);
    motions.saveAndFlush(motion);
    OfficialRecordOutcome certificationOutcome =
        passed ? OfficialRecordOutcome.PASSED : OfficialRecordOutcome.REJECTED;
    officialRecords.append(
        polityId,
        motion.getJurisdictionId(),
        constitution.getId(),
        requester.getId(),
        OfficialRecordType.MOTION_CERTIFIED,
        certification.getId(),
        OfficialRecordContext.certification(
            motion, PowerCode.REQUEST_CERTIFICATION, certification.getId(), certificationOutcome),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.MOTION_CERTIFIED,
            TemplateParameters.with(
                motion.getTemplateParams(),
                "motionTitle",
                motion.getTitle(),
                "motionTitleKey",
                motion.getTitleKey(),
                "motionBodyKey",
                motion.getBodyKey(),
                "outcome",
                certificationOutcome.value(),
                "outcomeReason",
                certification.getOutcomeReason().name(),
                "outcomeReasonKey",
                certification.getOutcomeReason().labelKey())),
        now);
    if (passed) {
      effects.apply(motion, requester, constitution, now);
    } else {
      officialRecords.append(
          polityId,
          motion.getJurisdictionId(),
          constitution.getId(),
          requester.getId(),
          OfficialRecordType.MOTION_REJECTED,
          motionId,
          OfficialRecordContext.certification(
              motion,
              PowerCode.REQUEST_CERTIFICATION,
              certification.getId(),
              OfficialRecordOutcome.REJECTED),
          OfficialRecordTemplate.of(
              OfficialRecordTemplateKey.MOTION_REJECTED,
              TemplateParameters.with(
                  motion.getTemplateParams(),
                  "motionTitle",
                  motion.getTitle(),
                  "motionTitleKey",
                  motion.getTitleKey(),
                  "motionBodyKey",
                  motion.getBodyKey())),
          now);
    }
    return context.result(motion, requester.getId());
  }

  private boolean canCertifyOwnAppealWithoutStanding(Motion motion, Membership requester) {
    return motion.getEffectType() == EffectType.GRANT_APPEAL
        && appealProposals
            .findProjectedByMotionId(motion.getId())
            .map(proposal -> proposal.getAppellantMembershipId().equals(requester.getId()))
            .orElse(false);
  }
}
