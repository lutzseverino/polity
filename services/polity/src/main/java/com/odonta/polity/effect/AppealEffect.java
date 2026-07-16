package com.odonta.polity.effect;

import com.odonta.common.api.ApiException;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.Appeal;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.AppealProposalProjection;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.service.OfficialRecordService;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class AppealEffect implements MotionEffect {
  private final AppealProposalRepository appealProposals;
  private final AppealRepository appeals;
  private final SanctionRepository sanctions;
  private final MembershipRepository memberships;
  private final OfficialRecordService officialRecords;

  @Override
  public EffectType type() {
    return EffectType.GRANT_APPEAL;
  }

  @Override
  public void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    AppealProposalProjection proposal =
        appealProposals
            .findProjectedByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "appeal_proposal_not_found", "Appeal proposal not found."));
    Sanction sanction =
        sanctions
            .findEntityByIdAndPolityId(proposal.getSanctionId(), motion.getPolityId())
            .orElseThrow(PolityResource.SANCTION::notFound);
    if (sanction.isInactiveAt(now)) {
      throw ApiException.conflict("sanction_not_active", "Only active sanctions can be appealed.");
    }
    if (appeals.existsByPolityIdAndSanctionId(motion.getPolityId(), sanction.getId())) {
      throw ApiException.conflict(
          "appeal_already_granted", "This sanction has already been appealed.");
    }
    sanction.vacate(now);
    sanctions.saveAndFlush(sanction);
    Appeal appeal =
        appeals.saveAndFlush(
            new Appeal(
                motion.getPolityId(),
                motion.getId(),
                sanction.getId(),
                proposal.getAppellantMembershipId(),
                proposal.getReason(),
                now));
    Membership appellant = membership(proposal.getAppellantMembershipId());
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.APPEAL_GRANTED,
        appeal.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.APPEAL_GRANTED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.APPEAL_GRANTED,
            TemplateParameters.of(
                "memberName", appellant.getDisplayName(), "reason", proposal.getReason())),
        now);
  }

  private Membership membership(UUID membershipId) {
    return memberships.findEntityById(membershipId).orElseThrow(PolityResource.MEMBER::notFound);
  }
}
