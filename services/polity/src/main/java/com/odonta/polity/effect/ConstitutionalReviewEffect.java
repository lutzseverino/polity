package com.odonta.polity.effect;

import com.odonta.common.api.ApiException;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalReview;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.ConstitutionalReviewProposalProjection;
import com.odonta.polity.repository.ConstitutionalReviewProposalRepository;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.service.OfficialRecordService;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class ConstitutionalReviewEffect implements MotionEffect {
  private final ConstitutionalReviewProposalRepository constitutionalReviewProposals;
  private final ConstitutionalReviewRepository constitutionalReviews;
  private final OfficialRecordRepository officialRecordEntries;
  private final OfficialActVoidRemedy officialActVoidRemedies;
  private final OfficialRecordService officialRecords;

  @Override
  public EffectType type() {
    return EffectType.VOID_OFFICIAL_ACT;
  }

  @Override
  public void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    ConstitutionalReviewProposalProjection proposal =
        constitutionalReviewProposals
            .findProjectedByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "constitutional_review_proposal_not_found",
                        "Constitutional review proposal not found."));
    OfficialRecordEntry target =
        officialRecordEntries
            .findEntityByIdAndPolityId(proposal.getTargetRecordId(), motion.getPolityId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "official_record_entry_not_found", "Official record entry not found."));
    if (!target.getType().isVoidableByConstitutionalReview()) {
      throw ApiException.conflict(
          "official_act_not_voidable",
          "This official act does not have a constitutional-review void remedy.");
    }
    if (constitutionalReviews.existsByPolityIdAndTargetRecordId(
        motion.getPolityId(), target.getId())) {
      throw ApiException.conflict(
          "constitutional_review_already_granted",
          "This official act has already been constitutionally reviewed.");
    }
    if (!officialActVoidRemedies.apply(target, now)) {
      throw ApiException.conflict(
          "official_act_void_remedy_unavailable",
          "This official act no longer has an active remedy to void.");
    }
    ConstitutionalReview review =
        constitutionalReviews.saveAndFlush(
            new ConstitutionalReview(
                motion.getPolityId(),
                motion.getId(),
                target.getId(),
                proposal.getPetitionerMembershipId(),
                proposal.getReason(),
                now));
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.OFFICIAL_ACT_VOIDED,
        review.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.VOIDED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.OFFICIAL_ACT_VOIDED,
            TemplateParameters.of(
                "entryNumber",
                target.getEntryNumber(),
                "targetType",
                target.getType().storedLabel(),
                "targetTypeCode",
                target.getType().wireValue(),
                "targetTypeKey",
                target.getType().labelKey(),
                "reason",
                proposal.getReason())),
        now);
  }
}
