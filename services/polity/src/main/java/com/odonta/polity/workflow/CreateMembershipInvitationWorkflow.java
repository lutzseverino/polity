package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.input.CreateMembershipInvitationInput;
import com.odonta.polity.integration.invite.CardoInvitationDispatch;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationStatus;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.resolver.ActiveMembershipResolver;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.result.MembershipInvitationResult;
import com.odonta.polity.service.MembershipInvitationService;
import com.odonta.polity.service.OfficialRecordService;
import com.odonta.polity.service.PolityActionAvailabilityService;
import com.odonta.polity.service.PolityService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@RequiredArgsConstructor
public class CreateMembershipInvitationWorkflow {
  private final Clock clock;
  private final ConstitutionalAuthority authority;
  private final CardoInvitationDispatch invitationDispatch;
  private final MembershipInvitationRepository invitations;
  private final ActiveMembershipResolver activeMemberships;
  private final MembershipRepository membershipRepository;
  private final MembershipInvitationService invitationResults;
  private final PolityContextResolver polityContext;
  private final PolityService polities;
  private final PolityActionAvailabilityService actionAvailability;
  private final OfficialRecordService officialRecords;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MembershipInvitationResult create(
      UUID polityId, AuthenticatedUser actor, @Valid CreateMembershipInvitationInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    String email = normalize(input.email());
    Membership inviter = activeMemberships.resolve(polityId, actor.id());
    ConstitutionVersion constitution = polityContext.constitution(polityId);
    requireAdmissionAuthority(inviter, constitution);
    if (invitations.existsByPolityIdAndEmailIgnoreCaseAndStatus(
        polityId, email, MembershipInvitationStatus.PENDING)) {
      throw ApiException.conflict(
          "invitation_exists", "This user already has a pending invitation.");
    }
    if (membershipRepository.existsByPolityIdAndEmailIgnoreCaseAndStatus(
        polityId, email, MembershipStatus.ACTIVE)) {
      throw ApiException.conflict("member_exists", "This user is already a member.");
    }
    MembershipInvitation invitation =
        invitations.saveAndFlush(new MembershipInvitation(polityId, email, inviter.getId(), now));
    Jurisdiction jurisdiction = polityContext.rootJurisdiction(polityId);
    officialRecords.append(
        polityId,
        jurisdiction.getId(),
        constitution.getId(),
        inviter.getId(),
        OfficialRecordType.MEMBER_INVITED,
        invitation.getId(),
        OfficialRecordContext.none(),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.MEMBER_INVITED,
            TemplateParameters.of(
                "inviterName", inviter.getDisplayName(), "inviteeEmail", invitation.getEmail())),
        now);
    invitationDispatch.stageCreation(invitation.getId(), actor.id());
    return invitationResults.get(invitation.getId());
  }

  private void requireAdmissionAuthority(Membership inviter, ConstitutionVersion constitution) {
    try {
      authority.require(inviter, constitution, PowerCode.ADMIT_MEMBER);
    } catch (ApiException exception) {
      if (!exception.code().equals("constitutional_authority_missing")
          || !actionAvailability.hasProvisionalFounderAdmissionAuthority(
              inviter.getPolityId(), inviter.getUserId())) {
        throw exception;
      }
    }
  }

  private String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
