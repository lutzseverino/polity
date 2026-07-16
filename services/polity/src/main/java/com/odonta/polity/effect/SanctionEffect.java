package com.odonta.polity.effect;

import com.odonta.common.api.ApiException;
import com.odonta.polity.exception.PolityResource;
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
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.SanctionProposalProjection;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.service.OfficialRecordService;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class SanctionEffect implements MotionEffect {
  private final SanctionProposalRepository sanctionProposals;
  private final SanctionRepository sanctions;
  private final MembershipRepository memberships;
  private final OfficialRecordService officialRecords;

  @Override
  public EffectType type() {
    return EffectType.APPLY_SANCTION;
  }

  @Override
  public void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    SanctionProposalProjection proposal =
        sanctionProposals
            .findProjectedByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "sanction_proposal_not_found", "Sanction proposal not found."));
    Sanction sanction =
        sanctions.saveAndFlush(
            new Sanction(
                motion.getPolityId(),
                motion.getId(),
                proposal.getTargetMembershipId(),
                proposal.getType(),
                proposal.getReason(),
                now,
                now.plusDays(proposal.getDurationDays())));
    Membership target = membership(proposal.getTargetMembershipId());
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.SANCTION_APPLIED,
        sanction.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.SANCTION_APPLIED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.SANCTION_APPLIED,
            TemplateParameters.of(
                "memberName",
                target.getDisplayName(),
                "sanctionType",
                proposal.getType().name(),
                "sanctionTypeKey",
                proposal.getType().labelKey(),
                "durationDays",
                proposal.getDurationDays(),
                "reason",
                proposal.getReason())),
        now);
  }

  private Membership membership(UUID membershipId) {
    return memberships.findEntityById(membershipId).orElseThrow(PolityResource.MEMBER::notFound);
  }
}
