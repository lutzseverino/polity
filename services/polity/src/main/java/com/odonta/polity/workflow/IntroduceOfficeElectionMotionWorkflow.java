package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.input.CreateOfficeElectionMotionInput;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionTemplate;
import com.odonta.polity.model.MotionTemplateKey;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionProposal;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.resolver.ActiveMembershipResolver;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.service.MembershipService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
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
public class IntroduceOfficeElectionMotionWorkflow {
  private final ConstitutionalAuthority authority;
  private final ActiveMembershipResolver activeMemberships;
  private final MembershipService membershipService;
  private final MotionIntroducer motions;
  private final OfficeElectionCandidateRepository candidates;
  private final OfficeElectionProposalRepository proposals;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult introduce(
      UUID polityId, AuthenticatedUser actor, @Valid CreateOfficeElectionMotionInput input) {
    MotionIntroductionContext context =
        motions.prepare(polityId, actor, ConstitutionalMotionPath.OFFICE_ELECTION);
    authority.require(
        context.introducer(), context.constitution(), context.path().introducingPower());
    Office office = currentOffice(input.officeId(), polityId, context.constitution());
    int seatsAvailable = requireOfficeVacancy(office, context.now());
    List<Membership> candidateMembers =
        activeCandidates(input.candidateMembershipIds(), polityId, context.now());
    List<String> candidateNames =
        candidateMembers.stream().map(Membership::getDisplayName).toList();
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.OFFICE_ELECTION,
            TemplateParameters.of(
                "officeName", office.getName(),
                "officeNameKey", office.getNameKey(),
                "officeCode", office.getCode(),
                "candidateNames", candidateNames,
                "candidateCount", candidateMembers.size()));
    Motion motion =
        motions.introduce(
            context, template.storedTitle(), template.storedBody(), template, Set.of());
    proposals.saveAndFlush(
        new OfficeElectionProposal(
            polityId,
            motion.getId(),
            office.getId(),
            seatsAvailable,
            context.procedure().getOfficeElectionMethod()));
    candidates.saveAllAndFlush(
        candidateMembers.stream()
            .map(
                candidate ->
                    new OfficeElectionCandidate(
                        polityId,
                        motion.getId(),
                        candidate.getId(),
                        candidate.getId().equals(context.introducer().getId())
                            ? OfficeElectionCandidateStatus.ACCEPTED
                            : OfficeElectionCandidateStatus.PENDING,
                        candidate.getId().equals(context.introducer().getId())
                            ? context.now()
                            : null))
            .toList());
    return motions.result(motion, context.introducer().getId());
  }

  private Office currentOffice(UUID officeId, UUID polityId, ConstitutionVersion constitution) {
    Office office =
        offices
            .findEntityByIdAndPolityId(officeId, polityId)
            .orElseThrow(PolityResource.OFFICE::notFound);
    if (!office.getConstitutionVersionId().equals(constitution.getId())) {
      throw ApiException.conflict(
          "office_not_current", "This office belongs to a previous constitution version.");
    }
    return office;
  }

  private int requireOfficeVacancy(Office office, OffsetDateTime now) {
    long activeTerms =
        officeTerms.countByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            office.getPolityId(), office.getCode(), OfficeTermStatus.ACTIVE, now);
    int vacantSeats = Math.max(0, office.getSeatCount() - Math.toIntExact(activeTerms));
    if (vacantSeats <= 0) {
      throw ApiException.conflict(
          "office_seats_full", "This office has no vacant seats for another active term.");
    }
    return vacantSeats;
  }

  private List<Membership> activeCandidates(
      List<UUID> candidateMembershipIds, UUID polityId, OffsetDateTime now) {
    Set<UUID> candidateIds = new HashSet<>();
    return candidateMembershipIds.stream()
        .map(
            candidateId -> {
              if (!candidateIds.add(candidateId)) {
                throw ApiException.badRequest(
                    "duplicate_candidate", "Each candidate can appear only once in an election.");
              }
              Membership candidate = activeMemberships.resolveById(polityId, candidateId);
              membershipService.requirePoliticalStanding(candidate.getId(), now);
              return candidate;
            })
        .toList();
  }
}
