package com.odonta.polity.workflow;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionElector;
import com.odonta.polity.model.MotionTemplate;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.resolver.ActiveMembershipResolver;
import com.odonta.polity.resolver.MotionResultResolver;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.resolver.ProcedureElectorateResolver;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.service.OfficialRecordService;
import com.odonta.polity.service.PolityService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class MotionIntroducer {
  private final Clock clock;
  private final ActiveMembershipResolver activeMemberships;
  private final InstitutionRepository institutions;
  private final MotionElectorRepository electors;
  private final MotionRepository motions;
  private final OfficialRecordService officialRecords;
  private final PolityContextResolver polityContext;
  private final PolityService polities;
  private final ProcedureElectorateResolver procedureElectorates;
  private final ProcedureRepository procedures;
  private final MotionResultResolver results;

  MotionIntroductionContext prepare(
      UUID polityId, AuthenticatedUser actor, ConstitutionalMotionPath path) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    Membership introducer = activeMemberships.resolve(polityId, actor.id());
    ConstitutionVersion constitution = polityContext.constitution(polityId);
    Jurisdiction jurisdiction = polityContext.rootJurisdiction(polityId);
    Procedure procedure =
        procedures
            .findEntityByConstitutionVersionIdAndCode(constitution.getId(), path.procedureCode())
            .orElseThrow(PolityResource.PROCEDURE::notFound);
    Institution institution =
        institutions
            .findEntityByIdAndPolityId(procedure.getInstitutionId(), polityId)
            .orElseThrow(PolityResource.INSTITUTION::notFound);
    return new MotionIntroductionContext(
        polityId, introducer, constitution, jurisdiction, institution, procedure, path, now);
  }

  Motion introduce(
      MotionIntroductionContext context,
      String title,
      String body,
      MotionTemplate template,
      Set<UUID> recusedMembershipIds) {
    Procedure procedure = context.procedure();
    OffsetDateTime votingOpensAt = context.now().plusHours(procedure.getMinimumNoticeHours());
    OffsetDateTime votingClosesAt = votingOpensAt.plusHours(procedure.getVotingPeriodHours());
    List<Membership> eligible =
        procedureElectorates.electors(procedure, votingOpensAt).stream()
            .filter(member -> !recusedMembershipIds.contains(member.getId()))
            .toList();
    requireEligibleElectorate(eligible);
    requireMinimumElectorate(eligible, procedure);
    Motion motion =
        motions.saveAndFlush(
            new Motion(
                context.polityId(),
                context.jurisdiction().getId(),
                context.institution().getId(),
                context.constitution().getId(),
                procedure.getId(),
                context.introducer().getId(),
                title,
                body,
                template,
                procedure.getEffectType(),
                context.now(),
                votingOpensAt,
                votingClosesAt,
                votingClosesAt));
    electors.saveAllAndFlush(
        eligible.stream()
            .map(member -> new MotionElector(context.polityId(), motion.getId(), member.getId()))
            .toList());
    officialRecords.append(
        context.polityId(),
        context.jurisdiction().getId(),
        context.constitution().getId(),
        context.introducer().getId(),
        OfficialRecordType.MOTION_INTRODUCED,
        motion.getId(),
        OfficialRecordContext.motion(
            motion, context.path().introducingPower(), OfficialRecordOutcome.INTRODUCED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.MOTION_INTRODUCED,
            introducedMotionParams(motion, title, context, votingOpensAt, eligible.size())),
        context.now());
    return motion;
  }

  MotionResult result(Motion motion, UUID currentMembershipId) {
    return results.resolve(motion, currentMembershipId);
  }

  private void requireEligibleElectorate(List<Membership> eligible) {
    if (eligible.isEmpty()) {
      throw ApiException.conflict(
          "procedure_electorate_empty",
          "This procedure has no eligible electors under the current constitution.");
    }
  }

  private void requireMinimumElectorate(List<Membership> eligible, Procedure procedure) {
    if (eligible.size() < procedure.getMinimumElectorCount()) {
      throw ApiException.conflict(
          "procedure_electorate_below_minimum",
          "This procedure does not have enough eligible electors under the current constitution.");
    }
  }

  private Map<String, Object> introducedMotionParams(
      Motion motion,
      String title,
      MotionIntroductionContext context,
      OffsetDateTime votingOpensAt,
      int eligibleElectorCount) {
    return TemplateParameters.with(
        motion.getTemplateParams(),
        "motionTitle",
        title,
        "motionTitleKey",
        motion.getTitleKey(),
        "motionBodyKey",
        motion.getBodyKey(),
        "motionTemplateParams",
        motion.getTemplateParams(),
        "introducerName",
        context.introducer().getDisplayName(),
        "procedureName",
        context.procedure().getName(),
        "procedureNameKey",
        context.procedure().getNameKey(),
        "procedureCode",
        context.procedure().getCode(),
        "institutionName",
        context.institution().getName(),
        "institutionNameKey",
        context.institution().getNameKey(),
        "institutionKind",
        context.institution().getKind().name(),
        "constitutionVersion",
        context.constitution().getVersion(),
        "votingOpensAt",
        votingOpensAt.toString(),
        "eligibleElectorCount",
        eligibleElectorCount);
  }
}
