package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.evaluator.ConstitutionAmendmentEvaluationException;
import com.odonta.polity.evaluator.ConstitutionAmendmentEvaluator;
import com.odonta.polity.input.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.input.CreateInstitutionChangeInput;
import com.odonta.polity.input.CreateOfficeChangeInput;
import com.odonta.polity.input.CreatePowerChangeInput;
import com.odonta.polity.input.CreateProcedureChangeInput;
import com.odonta.polity.input.ValidatedConstitutionAmendmentInput;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionInstitutionChangeProposal;
import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import com.odonta.polity.model.ConstitutionPowerChangeProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionTemplate;
import com.odonta.polity.model.MotionTemplateKey;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.resolver.ConstitutionAmendmentStateResolver;
import com.odonta.polity.result.MotionResult;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import jakarta.validation.Valid;
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
public class IntroduceConstitutionAmendmentMotionWorkflow {
  private final ConstitutionAmendmentEvaluator amendmentEvaluator;
  private final ConstitutionAmendmentProposalRepository amendmentProposals;
  private final ConstitutionAmendmentStateResolver amendmentStates;
  private final ConstitutionalAuthority authority;
  private final ConstitutionInstitutionChangeProposalRepository institutionChangeProposals;
  private final MotionIntroducer motions;
  private final ConstitutionOfficeChangeProposalRepository officeChangeProposals;
  private final ConstitutionPowerChangeProposalRepository powerChangeProposals;
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult introduce(
      UUID polityId, AuthenticatedUser actor, @Valid CreateConstitutionAmendmentMotionInput input) {
    MotionIntroductionContext context =
        motions.prepare(polityId, actor, ConstitutionalMotionPath.CONSTITUTION_AMENDMENT);
    authority.require(
        context.introducer(), context.constitution(), context.path().introducingPower());
    ValidatedConstitutionAmendmentInput plan = validatedPlan(context, input);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.CONSTITUTION_AMENDMENT,
            TemplateParameters.of("title", input.title(), "body", input.body()));
    Motion motion =
        motions.introduce(
            context, template.storedTitle(), template.storedBody(), template, Set.of());
    ConstitutionAmendmentProposal proposal =
        amendmentProposals.saveAndFlush(
            new ConstitutionAmendmentProposal(
                polityId, motion.getId(), input.title(), input.body()));
    institutionChangeProposals.saveAllAndFlush(
        plan.institutionChanges().stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    procedureChangeProposals.saveAllAndFlush(
        plan.procedureChanges().stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    officeChangeProposals.saveAllAndFlush(
        plan.officeChanges().stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    powerChangeProposals.saveAllAndFlush(
        plan.powerChanges().stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    return motions.result(motion, context.introducer().getId());
  }

  private ValidatedConstitutionAmendmentInput validatedPlan(
      MotionIntroductionContext context, CreateConstitutionAmendmentMotionInput input) {
    try {
      return amendmentEvaluator.evaluate(
          amendmentStates.resolve(context.constitution(), context.now()), input);
    } catch (ConstitutionAmendmentEvaluationException exception) {
      throw switch (exception.kind()) {
        case INVALID -> ApiException.badRequest(exception.code(), exception.getMessage());
        case MISSING_REFERENCE -> ApiException.notFound(exception.code(), exception.getMessage());
        case CONFLICTING_STATE -> ApiException.conflict(exception.code(), exception.getMessage());
      };
    }
  }

  private ConstitutionProcedureChangeProposal toProposal(
      UUID polityId, UUID amendmentProposalId, CreateProcedureChangeInput change) {
    return new ConstitutionProcedureChangeProposal(
        polityId,
        amendmentProposalId,
        change.procedureCode(),
        change.institutionId(),
        change.quorumNumerator(),
        change.quorumDenominator(),
        change.threshold(),
        change.officeElectionMethod(),
        change.electorate(),
        change.electorateOfficeCode(),
        change.minimumElectorCount(),
        change.minimumNoticeHours(),
        change.votingPeriodHours());
  }

  private ConstitutionOfficeChangeProposal toProposal(
      UUID polityId, UUID amendmentProposalId, CreateOfficeChangeInput change) {
    return new ConstitutionOfficeChangeProposal(
        polityId,
        amendmentProposalId,
        change.action(),
        change.code(),
        change.jurisdictionId(),
        change.name(),
        change.description(),
        change.termLengthDays(),
        change.seatCount());
  }

  private ConstitutionPowerChangeProposal toProposal(
      UUID polityId, UUID amendmentProposalId, CreatePowerChangeInput change) {
    return new ConstitutionPowerChangeProposal(
        polityId,
        amendmentProposalId,
        change.powerCode(),
        change.holderScope(),
        change.holderOfficeCode());
  }

  private ConstitutionInstitutionChangeProposal toProposal(
      UUID polityId, UUID amendmentProposalId, CreateInstitutionChangeInput change) {
    return new ConstitutionInstitutionChangeProposal(
        polityId,
        amendmentProposalId,
        change.action(),
        change.institutionId(),
        change.jurisdictionId(),
        change.name(),
        change.kind());
  }
}
