package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.model.Appeal;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeAssignmentProposal;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordCitation;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Resolution;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionProposal;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeAssignmentProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EffectApplicationService {
  private final AppealProposalRepository appealProposals;
  private final AppealRepository appeals;
  private final ConstitutionAmendmentProposalRepository amendmentProposals;
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalPowerRepository powers;
  private final InstitutionRepository institutions;
  private final MembershipRepository memberships;
  private final OfficeAssignmentProposalRepository officeAssignmentProposals;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficialRecordWriter record;
  private final ProcedureRepository procedures;
  private final ResolutionRepository resolutions;
  private final SanctionProposalRepository sanctionProposals;
  private final SanctionRepository sanctions;

  public void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    switch (motion.getEffectType()) {
      case ADOPT_RESOLUTION -> applyResolution(motion, actor, constitution, now);
      case ASSIGN_OFFICE -> assignOffice(motion, actor, constitution, now);
      case APPLY_SANCTION -> applySanction(motion, actor, constitution, now);
      case GRANT_APPEAL -> grantAppeal(motion, actor, constitution, now);
      case AMEND_CONSTITUTION -> amendConstitution(motion, actor, constitution, now);
    }
  }

  private void applyResolution(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    Resolution resolution =
        resolutions.saveAndFlush(
            new Resolution(
                motion.getPolityId(), motion.getId(), motion.getTitle(), motion.getBody(), now));
    record.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.RESOLUTION_ADOPTED,
        resolution.getId(),
        citation(motion, "adopted"),
        "Resolution adopted: " + motion.getTitle(),
        motion.getBody(),
        now);
  }

  private void assignOffice(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    OfficeAssignmentProposal proposal =
        officeAssignmentProposals
            .findByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "office_assignment_proposal_not_found",
                        "Office assignment proposal not found."));
    Office office =
        offices
            .findByIdAndPolityId(proposal.getOfficeId(), motion.getPolityId())
            .orElseThrow(() -> ApiException.notFound("office_not_found", "Office not found."));
    officeTerms
        .findByPolityIdAndOfficeCodeAndStatus(
            motion.getPolityId(), office.getCode(), OfficeTermStatus.ACTIVE)
        .forEach(term -> term.end(now));
    officeTerms.flush();
    OfficeTerm term =
        officeTerms.saveAndFlush(
            new OfficeTerm(
                motion.getPolityId(),
                office.getId(),
                office.getCode(),
                proposal.getNomineeMembershipId(),
                motion.getId(),
                now,
                now.plusDays(office.getTermLengthDays())));
    Membership nominee = member(proposal.getNomineeMembershipId());
    record.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.OFFICE_ASSIGNED,
        term.getId(),
        citation(motion, "office_assigned"),
        nominee.getDisplayName() + " assigned as " + office.getName(),
        "%s was assigned to the office of %s for %d days."
            .formatted(nominee.getDisplayName(), office.getName(), office.getTermLengthDays()),
        now);
  }

  private void applySanction(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    SanctionProposal proposal =
        sanctionProposals
            .findByMotionId(motion.getId())
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
    Membership target = member(proposal.getTargetMembershipId());
    record.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.SANCTION_APPLIED,
        sanction.getId(),
        citation(motion, "sanction_applied"),
        "Sanction applied to " + target.getDisplayName(),
        "%s received a %s sanction for %d days: %s"
            .formatted(
                target.getDisplayName(),
                proposal.getType().name().toLowerCase(),
                proposal.getDurationDays(),
                proposal.getReason()),
        now);
  }

  private void grantAppeal(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    AppealProposal proposal =
        appealProposals
            .findByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "appeal_proposal_not_found", "Appeal proposal not found."));
    Sanction sanction =
        sanctions
            .findByIdAndPolityId(proposal.getSanctionId(), motion.getPolityId())
            .orElseThrow(() -> ApiException.notFound("sanction_not_found", "Sanction not found."));
    if (!sanction.isActiveAt(now)) {
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
    Membership appellant = member(proposal.getAppellantMembershipId());
    record.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.APPEAL_GRANTED,
        appeal.getId(),
        citation(motion, "appeal_granted"),
        "Appeal granted for " + appellant.getDisplayName(),
        "The sanction was vacated after appeal: " + proposal.getReason(),
        now);
  }

  private void amendConstitution(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    ConstitutionAmendmentProposal proposal =
        amendmentProposals
            .findByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "amendment_proposal_not_found", "Amendment proposal not found."));
    ConstitutionVersion current =
        constitutions
            .findById(constitution.getId())
            .orElseThrow(
                () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
    Map<String, ConstitutionProcedureChangeProposal> procedureChanges =
        procedureChangeProposals.findByAmendmentProposalId(proposal.getId()).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ConstitutionProcedureChangeProposal::getProcedureCode, change -> change));
    if (procedureChanges.isEmpty()) {
      throw ApiException.conflict(
          "amendment_has_no_effect",
          "Constitution amendments must include at least one enforceable change.");
    }
    current.supersede();
    constitutions.saveAndFlush(current);
    ConstitutionVersion amended =
        constitutions.saveAndFlush(
            new ConstitutionVersion(
                motion.getPolityId(),
                current.getVersion() + 1,
                proposal.getTitle(),
                proposal.getBody(),
                now));
    copyInstitutions(current.getId(), amended.getId(), procedureChanges);
    offices
        .findByConstitutionVersionIdOrderByName(current.getId())
        .forEach(office -> offices.save(office.copyTo(amended.getId())));
    powers
        .findByConstitutionVersionId(current.getId())
        .forEach(power -> powers.save(power.copyTo(amended.getId())));
    record.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        current.getId(),
        actor.getId(),
        OfficialRecordType.CONSTITUTION_AMENDED,
        amended.getId(),
        citation(motion, "constitution_amended"),
        "Constitution amended to v" + amended.getVersion(),
        proposal.getBody() + "\n\nProcedure changes: " + proposal.getProcedureChangeSummary(),
        now);
  }

  private void copyInstitutions(
      UUID currentConstitutionId,
      UUID amendedConstitutionId,
      Map<String, ConstitutionProcedureChangeProposal> procedureChanges) {
    Map<UUID, UUID> institutionIds = new HashMap<>();
    institutions
        .findByConstitutionVersionId(currentConstitutionId)
        .forEach(
            institution -> {
              Institution copied =
                  institutions.saveAndFlush(institution.copyTo(amendedConstitutionId));
              institutionIds.put(institution.getId(), copied.getId());
            });
    procedures
        .findByConstitutionVersionId(currentConstitutionId)
        .forEach(
            procedure -> {
              UUID institutionId = institutionIds.get(procedure.getInstitutionId());
              if (institutionId != null) {
                ConstitutionProcedureChangeProposal change =
                    procedureChanges.get(procedure.getCode());
                if (change == null) {
                  procedures.save(procedure.copyTo(amendedConstitutionId, institutionId));
                } else {
                  procedures.save(
                      procedure.copyWithRules(
                          amendedConstitutionId,
                          institutionId,
                          valueOr(change.getQuorumNumerator(), procedure.getQuorumNumerator()),
                          valueOr(change.getQuorumDenominator(), procedure.getQuorumDenominator()),
                          valueOr(change.getThreshold(), procedure.getThreshold()),
                          valueOr(
                              change.getMinimumNoticeHours(), procedure.getMinimumNoticeHours()),
                          valueOr(
                              change.getVotingPeriodHours(), procedure.getVotingPeriodHours())));
                }
              }
            });
  }

  private int valueOr(Integer value, int fallback) {
    return value == null ? fallback : value;
  }

  private <T> T valueOr(T value, T fallback) {
    return value == null ? fallback : value;
  }

  private Membership member(UUID membershipId) {
    return memberships
        .findById(membershipId)
        .orElseThrow(() -> ApiException.notFound("member_not_found", "Member not found."));
  }

  private OfficialRecordCitation citation(Motion motion, String outcome) {
    return new OfficialRecordCitation(
        motion.getId(),
        motion.getProcedureId(),
        motion.getInstitutionId(),
        null,
        null,
        motion.getEffectType(),
        outcome);
  }
}
