package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.input.CastVoteInput;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.VoteRepository;
import com.odonta.polity.result.MotionResult;
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
public class CastMotionVoteWorkflow {
  private final MotionCommandContext context;
  private final MotionElectorRepository electors;
  private final OfficialRecordService officialRecords;
  private final VoteRepository votes;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult cast(
      UUID polityId, UUID motionId, AuthenticatedUser actor, @Valid CastVoteInput input) {
    OffsetDateTime now = context.now();
    Membership voter = context.activeMember(polityId, actor);
    Motion motion = context.motion(polityId, motionId);
    if (motion.getEffectType() == EffectType.ELECT_OFFICE) {
      throw ApiException.conflict(
          "office_election_ballot_required",
          "Office elections require an election ballot, not a yes/no vote.");
    }
    context.requireVoting(motion, now);
    if (!electors.existsByMotionIdAndMembershipId(motionId, voter.getId())) {
      throw ApiException.forbidden(
          "vote_ineligible", "This member was not eligible when voting opened.");
    }
    VoteChoice choice = input.choice();
    Vote vote =
        votes
            .findEntityByMotionIdAndMembershipId(motionId, voter.getId())
            .map(
                existing -> {
                  existing.replace(choice, now);
                  return existing;
                })
            .orElseGet(() -> new Vote(polityId, motionId, voter.getId(), choice, now));
    votes.saveAndFlush(vote);
    officialRecords.append(
        polityId,
        motion.getJurisdictionId(),
        motion.getConstitutionVersionId(),
        voter.getId(),
        OfficialRecordType.VOTE_CAST,
        motionId,
        OfficialRecordContext.motion(motion, OfficialRecordOutcome.VOTE_CAST),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.VOTE_CAST,
            TemplateParameters.with(
                motion.getTemplateParams(),
                "motionTitle",
                motion.getTitle(),
                "motionTitleKey",
                motion.getTitleKey(),
                "voterName",
                voter.getDisplayName())),
        now);
    return context.result(motion, voter.getId());
  }
}
