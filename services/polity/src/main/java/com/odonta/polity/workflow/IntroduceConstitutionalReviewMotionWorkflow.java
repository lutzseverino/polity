package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.effect.OfficialActVoidRemedy;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.input.CreateConstitutionalReviewMotionInput;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.ConstitutionalReviewProposal;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.MotionTemplate;
import com.odonta.polity.model.MotionTemplateKey;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.ConstitutionalReviewProposalRepository;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.result.MotionResult;
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
public class IntroduceConstitutionalReviewMotionWorkflow {
  private final ConstitutionalAuthority authority;
  private final ConstitutionalReviewProposalRepository constitutionalReviewProposals;
  private final ConstitutionalReviewRepository constitutionalReviews;
  private final MotionIntroducer introducer;
  private final MotionRepository motions;
  private final OfficeTermRepository officeTerms;
  private final OfficialActVoidRemedy officialActVoidRemedies;
  private final OfficialRecordRepository officialRecordEntries;
  private final SanctionRepository sanctions;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult introduce(
      UUID polityId, AuthenticatedUser actor, @Valid CreateConstitutionalReviewMotionInput input) {
    MotionIntroductionContext context =
        introducer.prepare(polityId, actor, ConstitutionalMotionPath.CONSTITUTIONAL_REVIEW);
    authority.require(
        context.introducer(), context.constitution(), context.path().introducingPower());
    OfficialRecordEntry target = voidableOfficialAct(polityId, input.targetRecordId());
    requireVoidRemedy(polityId, target, context.now());
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.CONSTITUTIONAL_REVIEW,
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
                input.reason()));
    Motion motion =
        introducer.introduce(
            context,
            template.storedTitle(),
            template.storedBody(),
            template,
            recusalsForOfficialAct(context.introducer().getId(), target));
    constitutionalReviewProposals.saveAndFlush(
        new ConstitutionalReviewProposal(
            polityId,
            motion.getId(),
            target.getId(),
            context.introducer().getId(),
            input.reason()));
    return introducer.result(motion, context.introducer().getId());
  }

  private OfficialRecordEntry voidableOfficialAct(UUID polityId, UUID recordId) {
    OfficialRecordEntry record =
        officialRecordEntries
            .findEntityByIdAndPolityId(recordId, polityId)
            .orElseThrow(PolityResource.OFFICIAL_RECORD_ENTRY::notFound);
    if (!record.getType().isVoidableByConstitutionalReview()) {
      throw ApiException.conflict(
          "official_act_not_reviewable",
          "This official act does not have a constitutional-review void remedy.");
    }
    return record;
  }

  private void requireVoidRemedy(UUID polityId, OfficialRecordEntry record, OffsetDateTime now) {
    boolean open =
        constitutionalReviewProposals
            .findProjectionsByPolityIdAndTargetRecordId(polityId, record.getId())
            .stream()
            .anyMatch(
                proposal ->
                    motions.existsByIdAndStatus(proposal.getMotionId(), MotionStatus.VOTING));
    if (open) {
      throw ApiException.conflict(
          "constitutional_review_already_open",
          "This official act already has an open constitutional review motion.");
    }
    if (constitutionalReviews.existsByPolityIdAndTargetRecordId(polityId, record.getId())) {
      throw ApiException.conflict(
          "constitutional_review_already_granted",
          "This official act has already been constitutionally reviewed.");
    }
    if (!officialActVoidRemedies.hasActiveRemedy(record, now)) {
      throw ApiException.conflict(
          "official_act_void_remedy_unavailable",
          "This official act no longer has an active remedy to void.");
    }
  }

  private Set<UUID> recusalsForOfficialAct(
      UUID petitionerMembershipId, OfficialRecordEntry record) {
    Set<UUID> recused = recusals(petitionerMembershipId, record.getActorMembershipId());
    if (record.getType() == OfficialRecordType.SANCTION_APPLIED) {
      sanctions
          .findEntityByIdAndPolityId(record.getSourceId(), record.getPolityId())
          .ifPresent(
              sanction -> {
                recused.add(sanction.getTargetMembershipId());
                recused.add(motion(record.getPolityId(), sanction.getMotionId()).getIntroducedBy());
              });
    }
    if (record.getType() == OfficialRecordType.OFFICE_ELECTED) {
      officeTerms
          .findEntityByIdAndPolityId(record.getSourceId(), record.getPolityId())
          .map(OfficeTerm::getMembershipId)
          .ifPresent(recused::add);
    }
    return recused;
  }

  private Motion motion(UUID polityId, UUID motionId) {
    return motions
        .findEntityByIdAndPolityId(motionId, polityId)
        .orElseThrow(PolityResource.MOTION::notFound);
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
