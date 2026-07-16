package com.odonta.polity.effect;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermReview;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProposalProjection;
import com.odonta.polity.repository.OfficeTermReviewProposalRepository;
import com.odonta.polity.repository.OfficeTermReviewRepository;
import com.odonta.polity.service.OfficialRecordService;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class OfficeTermReviewEffect implements MotionEffect {
  private final OfficeTermReviewProposalRepository officeTermReviewProposals;
  private final OfficeTermReviewRepository officeTermReviews;
  private final OfficeTermRepository officeTerms;
  private final OfficeRepository offices;
  private final MembershipRepository memberships;
  private final OfficialRecordService officialRecords;

  @Override
  public EffectType type() {
    return EffectType.VACATE_OFFICE_TERM;
  }

  @Override
  public void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    OfficeTermReviewProposalProjection proposal =
        officeTermReviewProposals
            .findProjectedByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "office_term_review_proposal_not_found",
                        "Office term review proposal not found."));
    OfficeTerm term =
        officeTerms
            .findEntityByIdAndPolityId(proposal.getOfficeTermId(), motion.getPolityId())
            .orElseThrow(PolityResource.OFFICE_TERM::notFound);
    if (term.getStatus() != OfficeTermStatus.ACTIVE || !term.getEndsAt().isAfter(now)) {
      throw ApiException.conflict(
          "office_term_not_vacatable", "Only active office terms can be vacated.");
    }
    if (officeTermReviews.existsByPolityIdAndOfficeTermId(motion.getPolityId(), term.getId())) {
      throw ApiException.conflict(
          "office_term_review_already_granted", "This office term has already been reviewed.");
    }
    term.end(now);
    officeTerms.saveAndFlush(term);
    OfficeTermReview review =
        officeTermReviews.saveAndFlush(
            new OfficeTermReview(
                motion.getPolityId(),
                motion.getId(),
                term.getId(),
                proposal.getPetitionerMembershipId(),
                proposal.getReason(),
                now));
    Office office =
        offices
            .findEntityByIdAndPolityId(term.getOfficeId(), motion.getPolityId())
            .orElseThrow(PolityResource.OFFICE::notFound);
    Membership holder = membership(term.getMembershipId());
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.OFFICE_TERM_VACATED,
        review.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.OFFICE_TERM_VACATED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.OFFICE_TERM_VACATED,
            TemplateParameters.of(
                "memberName",
                holder.getDisplayName(),
                "officeName",
                office.getName(),
                "officeNameKey",
                office.getNameKey(),
                "officeCode",
                office.getCode(),
                "reason",
                proposal.getReason())),
        now);
  }

  private Membership membership(UUID membershipId) {
    return memberships.findEntityById(membershipId).orElseThrow(PolityResource.MEMBER::notFound);
  }
}
