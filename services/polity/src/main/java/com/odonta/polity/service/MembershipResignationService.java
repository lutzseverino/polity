package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityRevocationPlanner;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.resolver.GovernmentAssessmentResolver;
import io.github.lutzseverino.cardo.authorization.grant.Revocations;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipResignationService {
  private final Clock clock;
  private final ConstitutionVersionRepository constitutions;
  private final GovernmentAssessmentResolver governmentAssessments;
  private final JurisdictionRepository jurisdictions;
  private final MembershipRepository memberships;
  private final OfficeTermRepository officeTerms;
  private final OfficialRecordService officialRecords;
  private final PolityRepository polities;
  private final PolityRevocationPlanner revocationPlanner;
  private final Revocations revocations;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public void resign(UUID polityId, AuthenticatedUser actor) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Polity polity =
        polities.findEntityByIdForUpdate(polityId).orElseThrow(PolityResource.POLITY::notFound);
    if (polity.getStatus() != PolityStatus.ACTIVE) {
      throw ApiException.conflict(
          "polity_disbanded", "This polity has been disbanded and no longer accepts actions.");
    }
    Membership member =
        memberships
            .findEntityByPolityIdAndUserIdAndStatus(polityId, actor.id(), MembershipStatus.ACTIVE)
            .orElseThrow(
                () ->
                    ApiException.forbidden(
                        "polity_membership_required", "Active membership is required."));
    ConstitutionVersion constitution = constitution(polityId);
    long activeMembers = memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE);
    boolean closePolity =
        activeMembers <= 1
            && governmentAssessments.lastMemberResignationClosesPolity(polity, constitution);
    if (activeMembers <= 1 && !closePolity) {
      throw ApiException.conflict(
          "last_member_resignation_unavailable",
          "The last active citizen must disband the polity instead of resigning.");
    }
    if (!closePolity
        && !polity.isBootstrapComplete()
        && polity.getFounderId().equals(member.getUserId())) {
      throw ApiException.conflict(
          "provisional_founder_resignation_unavailable",
          "The founding citizen cannot resign before the polity reaches full government size.");
    }
    member.resign(now);
    memberships.saveAndFlush(member);
    List<OfficeTerm> activeTerms =
        officeTerms.findEntitiesByPolityIdAndMembershipIdAndStatus(
            polityId, member.getId(), OfficeTermStatus.ACTIVE);
    activeTerms.forEach(term -> term.end(now));
    if (!activeTerms.isEmpty()) {
      officeTerms.saveAllAndFlush(activeTerms);
    }
    revocations.stage(revocationPlanner.membership(member.getAuthorizationSubject(), polityId));
    if (closePolity) {
      polity.disband(now);
      polities.saveAndFlush(polity);
    }
    Jurisdiction jurisdiction = jurisdiction(polityId);
    officialRecords.append(
        polityId,
        jurisdiction.getId(),
        constitution.getId(),
        member.getId(),
        OfficialRecordType.MEMBER_RESIGNED,
        member.getId(),
        OfficialRecordContext.none(),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.MEMBER_RESIGNED,
            TemplateParameters.of("memberName", member.getDisplayName())),
        now);
    if (closePolity) {
      officialRecords.append(
          polityId,
          jurisdiction.getId(),
          constitution.getId(),
          member.getId(),
          OfficialRecordType.POLITY_DISBANDED,
          polity.getId(),
          OfficialRecordContext.none(),
          OfficialRecordTemplate.of(
              OfficialRecordTemplateKey.POLITY_DISBANDED_BY_LAST_RESIGNATION,
              TemplateParameters.of("polityName", polity.getName())),
          now);
    }
  }

  private ConstitutionVersion constitution(UUID polityId) {
    return constitutions
        .findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED)
        .orElseThrow(PolityResource.CONSTITUTION::notFound);
  }

  private Jurisdiction jurisdiction(UUID polityId) {
    return jurisdictions
        .findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT)
        .orElseThrow(PolityResource.JURISDICTION::notFound);
  }
}
