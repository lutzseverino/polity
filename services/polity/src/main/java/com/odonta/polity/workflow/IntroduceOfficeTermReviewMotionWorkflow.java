package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.input.CreateOfficeTermReviewMotionInput;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.MotionTemplate;
import com.odonta.polity.model.MotionTemplateKey;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermReviewProposal;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProposalRepository;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.service.MembershipService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
public class IntroduceOfficeTermReviewMotionWorkflow {
  private final ConstitutionalAuthority authority;
  private final MembershipService memberships;
  private final MotionIntroducer introducer;
  private final MotionRepository motions;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficeTermReviewProposalRepository proposals;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult introduce(
      UUID polityId, AuthenticatedUser actor, @Valid CreateOfficeTermReviewMotionInput input) {
    MotionIntroductionContext context =
        introducer.prepare(polityId, actor, ConstitutionalMotionPath.OFFICE_TERM_REVIEW);
    authority.require(
        context.introducer(), context.constitution(), context.path().introducingPower());
    OfficeTerm term = reviewableOfficeTerm(input.officeTermId(), polityId, context.now());
    requireNoOpenReview(polityId, term.getId());
    Office office =
        offices
            .findEntityByIdAndPolityId(term.getOfficeId(), polityId)
            .orElseThrow(PolityResource.OFFICE::notFound);
    String officeHolderName = memberships.displayName(term.getMembershipId());
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.OFFICE_TERM_REVIEW,
            TemplateParameters.of(
                "officeTermId", term.getId().toString(),
                "officeName", office.getName(),
                "officeNameKey", office.getNameKey(),
                "officeCode", office.getCode(),
                "memberName", officeHolderName,
                "reason", input.reason()));
    Motion motion =
        introducer.introduce(
            context,
            template.storedTitle(),
            template.storedBody(),
            template,
            recusals(context.introducer().getId(), term.getMembershipId()));
    proposals.saveAndFlush(
        new OfficeTermReviewProposal(
            polityId, motion.getId(), term.getId(), context.introducer().getId(), input.reason()));
    return introducer.result(motion, context.introducer().getId());
  }

  private OfficeTerm reviewableOfficeTerm(UUID officeTermId, UUID polityId, OffsetDateTime now) {
    OfficeTerm term =
        officeTerms
            .findEntityByIdAndPolityId(officeTermId, polityId)
            .orElseThrow(PolityResource.OFFICE_TERM::notFound);
    if (term.getStatus() != OfficeTermStatus.ACTIVE || !term.getEndsAt().isAfter(now)) {
      throw ApiException.conflict(
          "office_term_not_reviewable", "Only active office terms can be reviewed.");
    }
    return term;
  }

  private void requireNoOpenReview(UUID polityId, UUID officeTermId) {
    boolean open =
        proposals.findProjectionsByPolityIdAndOfficeTermId(polityId, officeTermId).stream()
            .anyMatch(
                proposal ->
                    motions.existsByIdAndStatus(proposal.getMotionId(), MotionStatus.VOTING));
    if (open) {
      throw ApiException.conflict(
          "office_term_review_already_open",
          "This office term already has an open office term review motion.");
    }
  }

  private Set<UUID> recusals(UUID... membershipIds) {
    Set<UUID> recusals = new HashSet<>();
    for (UUID membershipId : membershipIds) {
      if (membershipId != null) {
        recusals.add(membershipId);
      }
    }
    return recusals;
  }
}
