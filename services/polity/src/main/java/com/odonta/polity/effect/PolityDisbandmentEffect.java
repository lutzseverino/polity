package com.odonta.polity.effect;

import com.odonta.authorization.grant.Revocations;
import com.odonta.common.api.ApiException;
import com.odonta.polity.authorization.PolityRevocationPlanner;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.service.OfficialRecordService;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class PolityDisbandmentEffect implements MotionEffect {
  private final PolityRepository polities;
  private final OfficeTermRepository officeTerms;
  private final MembershipRepository memberships;
  private final OfficialRecordService officialRecords;
  private final PolityRevocationPlanner revocationPlanner;
  private final Revocations revocations;

  @Override
  public EffectType type() {
    return EffectType.DISBAND_POLITY;
  }

  @Override
  public void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    Polity polity =
        polities
            .findEntityById(motion.getPolityId())
            .orElseThrow(() -> ApiException.notFound("polity_not_found", "Polity not found."));
    if (polity.isDisbanded()) {
      throw ApiException.conflict(
          "polity_already_disbanded", "This polity has already been disbanded.");
    }
    officeTerms
        .findEntitiesByPolityIdAndStatus(motion.getPolityId(), OfficeTermStatus.ACTIVE)
        .forEach(term -> term.end(now));
    officeTerms.flush();
    memberships
        .findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            motion.getPolityId(), MembershipStatus.ACTIVE)
        .forEach(
            member ->
                revocations.stage(
                    revocationPlanner.participation(
                        member.getAuthorizationSubject(), motion.getPolityId())));
    polity.disband(now);
    polities.saveAndFlush(polity);
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.POLITY_DISBANDED,
        polity.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.POLITY_DISBANDED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.POLITY_DISBANDED,
            TemplateParameters.with(
                motion.getTemplateParams(),
                "polityName",
                polity.getName(),
                "motionBody",
                motion.getBody(),
                "motionBodyKey",
                motion.getBodyKey())),
        now);
  }
}
