package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.input.CastOfficeElectionBallotInput;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficeElectionBallot;
import com.odonta.polity.model.OfficeElectionBallotPreference;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.OfficeElectionBallotPreferenceRepository;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.service.OfficialRecordService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
public class CastOfficeElectionBallotWorkflow {
  private final OfficeElectionBallotRepository ballots;
  private final OfficeElectionBallotPreferenceRepository ballotPreferences;
  private final OfficeElectionCandidateRepository candidates;
  private final MotionCommandContext context;
  private final MotionElectorRepository electors;
  private final OfficialRecordService officialRecords;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult cast(
      UUID polityId,
      UUID motionId,
      AuthenticatedUser actor,
      @Valid CastOfficeElectionBallotInput input) {
    OffsetDateTime now = context.now();
    Membership voter = context.activeMember(polityId, actor);
    Motion motion = context.motion(polityId, motionId);
    if (motion.getEffectType() != EffectType.ELECT_OFFICE) {
      throw ApiException.conflict(
          "motion_not_office_election", "This motion is not an office election.");
    }
    context.requireVoting(motion, now);
    if (!electors.existsByMotionIdAndMembershipId(motionId, voter.getId())) {
      throw ApiException.forbidden(
          "vote_ineligible", "This member was not eligible when voting opened.");
    }
    List<UUID> rankedCandidateMembershipIds = rankedCandidateMembershipIds(input);
    requireAcceptedCandidates(motionId, rankedCandidateMembershipIds);
    OfficeElectionBallot ballot =
        ballots
            .findEntityByMotionIdAndMembershipId(motionId, voter.getId())
            .map(
                existing -> {
                  existing.replace(now);
                  return existing;
                })
            .orElseGet(() -> new OfficeElectionBallot(polityId, motionId, voter.getId(), now));
    ballot = ballots.saveAndFlush(ballot);
    ballotPreferences.deleteByBallotId(ballot.getId());
    ballotPreferences.saveAllAndFlush(
        preferences(polityId, motionId, ballot, rankedCandidateMembershipIds));
    officialRecords.append(
        polityId,
        motion.getJurisdictionId(),
        motion.getConstitutionVersionId(),
        voter.getId(),
        OfficialRecordType.VOTE_CAST,
        motionId,
        OfficialRecordContext.motion(motion, OfficialRecordOutcome.ELECTION_BALLOT_CAST),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.OFFICE_ELECTION_BALLOT_CAST,
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

  private List<UUID> rankedCandidateMembershipIds(CastOfficeElectionBallotInput input) {
    List<UUID> candidateMembershipIds = input.candidateMembershipIds();
    if (candidateMembershipIds == null || candidateMembershipIds.isEmpty()) {
      throw ApiException.badRequest(
          "office_election_ranking_required",
          "Office election ballots must rank at least one accepted candidate.");
    }
    if (new LinkedHashSet<>(candidateMembershipIds).size() != candidateMembershipIds.size()) {
      throw ApiException.badRequest(
          "office_election_ranking_duplicate",
          "Each accepted candidate can appear only once on an election ballot.");
    }
    return candidateMembershipIds;
  }

  private void requireAcceptedCandidates(UUID motionId, List<UUID> candidateMembershipIds) {
    for (UUID candidateMembershipId : candidateMembershipIds) {
      if (!candidates.existsByMotionIdAndMembershipIdAndStatus(
          motionId, candidateMembershipId, OfficeElectionCandidateStatus.ACCEPTED)) {
        throw ApiException.badRequest(
            "candidate_not_accepted", "This member is not an accepted candidate in the election.");
      }
    }
  }

  private List<OfficeElectionBallotPreference> preferences(
      UUID polityId,
      UUID motionId,
      OfficeElectionBallot ballot,
      List<UUID> candidateMembershipIds) {
    List<OfficeElectionBallotPreference> preferences = new ArrayList<>();
    for (int index = 0; index < candidateMembershipIds.size(); index++) {
      preferences.add(
          new OfficeElectionBallotPreference(
              polityId,
              motionId,
              ballot.getId(),
              ballot.getMembershipId(),
              candidateMembershipIds.get(index),
              index + 1));
    }
    return preferences;
  }
}
