package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.input.RespondOfficeElectionCandidacyInput;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.service.MembershipService;
import com.odonta.polity.service.OfficialRecordService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
public class RespondOfficeElectionCandidacyWorkflow {
  private final OfficeElectionCandidateRepository candidates;
  private final MotionCommandContext context;
  private final MembershipService memberships;
  private final OfficialRecordService officialRecords;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult respond(
      UUID polityId,
      UUID motionId,
      AuthenticatedUser actor,
      @Valid RespondOfficeElectionCandidacyInput input) {
    OffsetDateTime now = context.now();
    Membership candidate = context.activeMember(polityId, actor);
    Motion motion = context.motion(polityId, motionId);
    if (motion.getEffectType() != EffectType.ELECT_OFFICE) {
      throw ApiException.conflict(
          "motion_not_office_election", "This motion is not an office election.");
    }
    context.requireCandidacyResponseOpen(motion, now);
    OfficeElectionCandidate candidacy =
        candidates
            .findEntityByMotionIdAndMembershipId(motionId, candidate.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "candidacy_not_found", "This member is not nominated in the election."));
    if (input.accepted()) {
      memberships.requirePoliticalStanding(candidate.getId(), now);
    }
    candidacy.respond(input.accepted(), now);
    candidates.saveAndFlush(candidacy);
    OfficialRecordTemplateKey responseTemplateKey =
        input.accepted()
            ? OfficialRecordTemplateKey.CANDIDACY_ACCEPTED
            : OfficialRecordTemplateKey.CANDIDACY_DECLINED;
    OfficialRecordOutcome outcome =
        input.accepted()
            ? OfficialRecordOutcome.CANDIDACY_ACCEPTED
            : OfficialRecordOutcome.CANDIDACY_DECLINED;
    officialRecords.append(
        polityId,
        motion.getJurisdictionId(),
        motion.getConstitutionVersionId(),
        candidate.getId(),
        OfficialRecordType.CANDIDACY_RESPONDED,
        motionId,
        OfficialRecordContext.motion(motion, outcome),
        OfficialRecordTemplate.of(
            responseTemplateKey,
            TemplateParameters.with(
                motion.getTemplateParams(),
                "motionTitle",
                motion.getTitle(),
                "motionTitleKey",
                motion.getTitleKey(),
                "candidateName",
                candidate.getDisplayName())),
        now);
    return context.result(motion, candidate.getId());
  }
}
