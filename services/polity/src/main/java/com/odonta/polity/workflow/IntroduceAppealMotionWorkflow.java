package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.input.CreateAppealMotionInput;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.MotionTemplate;
import com.odonta.polity.model.MotionTemplateKey;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.MotionRepository;
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
public class IntroduceAppealMotionWorkflow {
  private final AppealRepository appeals;
  private final AppealProposalRepository appealProposals;
  private final ConstitutionalAuthority authority;
  private final MotionIntroducer introducer;
  private final MotionRepository motions;
  private final SanctionRepository sanctions;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult introduce(
      UUID polityId, AuthenticatedUser actor, @Valid CreateAppealMotionInput input) {
    MotionIntroductionContext context =
        introducer.prepare(polityId, actor, ConstitutionalMotionPath.APPEAL);
    Sanction sanction =
        sanctions
            .findEntityByIdAndPolityId(input.sanctionId(), polityId)
            .orElseThrow(PolityResource.SANCTION::notFound);
    requireAppealable(polityId, sanction, context.now());
    if (sanction.getTargetMembershipId().equals(context.introducer().getId())) {
      authority.requireOwnAppealIntroduction(context.introducer(), context.constitution());
    } else {
      authority.require(
          context.introducer(), context.constitution(), context.path().introducingPower());
    }
    UUID sanctionIntroducerId = motion(polityId, sanction.getMotionId()).getIntroducedBy();
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.APPEAL,
            TemplateParameters.of(
                "reason", input.reason(), "sanctionId", sanction.getId().toString()));
    Motion motion =
        introducer.introduce(
            context,
            template.storedTitle(),
            template.storedBody(),
            template,
            recusals(
                sanction.getTargetMembershipId(),
                context.introducer().getId(),
                sanctionIntroducerId));
    appealProposals.saveAndFlush(
        new AppealProposal(
            polityId,
            motion.getId(),
            sanction.getId(),
            sanction.getTargetMembershipId(),
            input.reason()));
    return introducer.result(motion, context.introducer().getId());
  }

  private void requireAppealable(UUID polityId, Sanction sanction, OffsetDateTime now) {
    if (sanction.isInactiveAt(now)) {
      throw ApiException.conflict("sanction_not_active", "Only active sanctions can be appealed.");
    }
    if (appeals.existsByPolityIdAndSanctionId(polityId, sanction.getId())) {
      throw ApiException.conflict(
          "appeal_already_granted", "This sanction has already been appealed.");
    }
    boolean open =
        appealProposals.findProjectionsByPolityIdAndSanctionId(polityId, sanction.getId()).stream()
            .anyMatch(
                proposal ->
                    motions.existsByIdAndStatus(proposal.getMotionId(), MotionStatus.VOTING));
    if (open) {
      throw ApiException.conflict(
          "appeal_already_open", "This sanction already has an open appeal motion.");
    }
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
