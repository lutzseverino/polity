package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.input.CreateSanctionMotionInput;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionTemplate;
import com.odonta.polity.model.MotionTemplateKey;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.SanctionProposal;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.resolver.ActiveMembershipResolver;
import com.odonta.polity.resolver.ProcedureElectorateResolver;
import com.odonta.polity.result.ActionAvailabilityResult;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.service.PolityActionAvailabilityService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Map;
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
public class IntroduceSanctionMotionWorkflow {
  private final PolityActionAvailabilityService actionAvailability;
  private final ActiveMembershipResolver activeMemberships;
  private final ConstitutionalAuthority authority;
  private final MotionIntroducer motions;
  private final ProcedureElectorateResolver procedureElectorates;
  private final ProcedureRepository procedures;
  private final SanctionProposalRepository proposals;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult introduce(
      UUID polityId, AuthenticatedUser actor, @Valid CreateSanctionMotionInput input) {
    MotionIntroductionContext context =
        motions.prepare(polityId, actor, ConstitutionalMotionPath.SANCTION);
    authority.require(
        context.introducer(), context.constitution(), context.path().introducingPower());
    Membership target = activeMemberships.resolveById(polityId, input.targetMembershipId());
    requireSanctionSafeguards(context, target, input.durationDays());
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.SANCTION,
            TemplateParameters.of(
                "targetName", target.getDisplayName(),
                "sanctionType", input.type().name(),
                "sanctionTypeKey", input.type().labelKey(),
                "reason", input.reason(),
                "durationDays", input.durationDays()));
    Motion motion =
        motions.introduce(
            context, template.storedTitle(), template.storedBody(), template, Set.of());
    proposals.saveAndFlush(
        new SanctionProposal(
            polityId,
            motion.getId(),
            target.getId(),
            input.type(),
            input.reason(),
            input.durationDays()));
    return motions.result(motion, context.introducer().getId());
  }

  private void requireSanctionSafeguards(
      MotionIntroductionContext context, Membership target, int durationDays) {
    ActionAvailabilityResult availability =
        actionAvailability.sanctionAvailability(
            context.introducer().getPolityId(), context.introducer().getUserId());
    if (!availability.available()) {
      throw ApiException.conflict(
          availability.reason().wireValue(), "Sanctions require an available appeal procedure.");
    }
    Procedure appealProcedure =
        procedures
            .findEntityByConstitutionVersionIdAndCode(
                context.constitution().getId(), Procedure.APPEAL)
            .orElseThrow(PolityResource.PROCEDURE::notFound);
    int minimumDurationDays = minimumAppealableSanctionDurationDays(appealProcedure);
    if (durationDays < minimumDurationDays) {
      throw ApiException.badRequest(
          "sanction_duration_too_short",
          "Sanctions must last long enough for an appeal to be completed.",
          Map.of("minimumDurationDays", minimumDurationDays));
    }
    OffsetDateTime appealVotingOpensAt =
        context.now().plusHours(appealProcedure.getMinimumNoticeHours());
    var eligible =
        procedureElectorates.electors(appealProcedure, appealVotingOpensAt).stream()
            .filter(member -> !member.getId().equals(target.getId()))
            .filter(member -> !member.getId().equals(context.introducer().getId()))
            .toList();
    if (eligible.size() < appealProcedure.getMinimumElectorCount()) {
      throw ApiException.conflict(
          "appeal_procedure_conflict_recusal_unavailable",
          "Sanctions require an available appeal procedure after conflict recusal.");
    }
  }

  private int minimumAppealableSanctionDurationDays(Procedure appealProcedure) {
    int appealHours =
        appealProcedure.getMinimumNoticeHours() + appealProcedure.getVotingPeriodHours();
    return appealHours == 0 ? 1 : appealHours / 24 + 1;
  }
}
