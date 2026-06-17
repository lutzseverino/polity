package com.odonta.polity.service;

import com.odonta.authorization.grant.Grants;
import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.identity.client.IdentityUser;
import com.odonta.identity.client.IdentityUsersClient;
import com.odonta.identity.client.ProvisionalUser;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.mapper.InvitationApplicationMapper;
import com.odonta.polity.mapper.PolityApplicationMapper;
import com.odonta.polity.model.CreateMemberInvitationInput;
import com.odonta.polity.model.InvitationStatus;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationResult;
import com.odonta.polity.model.MembershipResult;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class InvitationService {
  private final Clock clock;
  private final ConstitutionalAuthority authority;
  private final Grants grants;
  private final IdentityUsersClient identityUsers;
  private final InvitationApplicationMapper invitationMapper;
  private final MembershipInvitationRepository invitations;
  private final MembershipReader membershipReader;
  private final MembershipRepository memberships;
  private final PolityApplicationMapper memberMapper;
  private final PolityGrantPlanner grantPlanner;
  private final PolityService polities;
  private final OfficialRecordWriter record;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MembershipInvitationResult create(
      UUID polityId, AuthenticatedUser actor, @Valid CreateMemberInvitationInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    String email = normalize(input.email());
    Membership inviter = membershipReader.active(polityId, actor.id());
    var constitution = polities.constitution(polityId);
    authority.require(inviter, constitution, PowerCode.ADMIT_MEMBER);
    if (invitations.existsByPolityIdAndEmailIgnoreCaseAndStatus(
        polityId, email, InvitationStatus.PENDING)) {
      throw ApiException.conflict(
          "invitation_exists", "This user already has a pending invitation.");
    }
    ProvisionalUser invitee = identityUsers.createProvisional(email);
    if (memberships.existsByPolityIdAndUserId(polityId, invitee.id())) {
      throw ApiException.conflict("member_exists", "This user is already a member.");
    }
    if (invitations.existsByPolityIdAndInvitedUserIdAndStatus(
        polityId, invitee.id(), InvitationStatus.PENDING)) {
      throw ApiException.conflict(
          "invitation_exists", "This user already has a pending invitation.");
    }
    MembershipInvitation invitation =
        invitations.saveAndFlush(
            new MembershipInvitation(
                polityId,
                invitee.id(),
                invitee.authorizationSubject(),
                email,
                inviter.getId(),
                now));
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    record.append(
        polityId,
        jurisdiction.getId(),
        constitution.getId(),
        inviter.getId(),
        OfficialRecordType.MEMBER_INVITED,
        invitation.getId(),
        invitation.getEmail() + " was invited",
        "%s invited %s to become a citizen."
            .formatted(inviter.getDisplayName(), invitation.getEmail()),
        now);
    return result(invitation.getId());
  }

  @PreAuthorize(PolityPermissions.HAS_POLITY_READ)
  public List<MembershipInvitationResult> listPolityInvitations(UUID polityId, UUID userId) {
    membershipReader.active(polityId, userId);
    return invitationMapper.toResults(invitations.findProjectionsByPolityId(polityId));
  }

  public List<MembershipInvitationResult> listCurrentUserInvitations(AuthenticatedUser actor) {
    IdentityUser user = identityUsers.get(actor.id());
    return invitationMapper.toResults(
        invitations.findPendingProjectionsForInvitee(
            user.id(), List.of(normalize(user.email())), InvitationStatus.PENDING));
  }

  @Transactional
  public MembershipResult accept(UUID invitationId, AuthenticatedUser actor) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    IdentityUser identity = identityUsers.get(actor.id());
    MembershipInvitation invitation =
        invitations
            .findByIdAndStatus(invitationId, InvitationStatus.PENDING)
            .orElseThrow(
                () ->
                    ApiException.notFound("invitation_not_found", "Pending invitation not found."));
    requireInvitee(invitation, identity);
    if (memberships.existsByPolityIdAndUserId(invitation.getPolityId(), identity.id())) {
      throw ApiException.conflict("member_exists", "This user is already a member.");
    }
    Membership admitted =
        memberships.saveAndFlush(
            new Membership(
                invitation.getPolityId(),
                identity.id(),
                identity.authorizationSubject(),
                identity.email(),
                displayName(identity),
                now,
                invitation.getInvitedBy()));
    invitation.accept(now);
    invitations.saveAndFlush(invitation);
    grants.stage(
        grantPlanner.membership(identity.authorizationSubject(), invitation.getPolityId()));
    var constitution = polities.constitution(invitation.getPolityId());
    Jurisdiction jurisdiction = polities.jurisdiction(invitation.getPolityId());
    record.append(
        invitation.getPolityId(),
        jurisdiction.getId(),
        constitution.getId(),
        admitted.getId(),
        OfficialRecordType.MEMBER_ADMITTED,
        admitted.getId(),
        admitted.getDisplayName() + " accepted membership",
        "%s accepted the pending invitation and became an active citizen."
            .formatted(admitted.getDisplayName()),
        now);
    return memberMapper.toResult(
        memberships
            .findProjectedById(admitted.getId())
            .orElseThrow(() -> ApiException.notFound("member_not_found", "Member not found.")));
  }

  private MembershipInvitationResult result(UUID invitationId) {
    return invitationMapper.toResult(
        invitations
            .findProjectedById(invitationId)
            .orElseThrow(
                () -> ApiException.notFound("invitation_not_found", "Invitation not found.")));
  }

  private void requireInvitee(MembershipInvitation invitation, IdentityUser identity) {
    if (invitation.getInvitedUserId().equals(identity.id())
        || invitation.getEmail().equals(normalize(identity.email()))) {
      return;
    }
    throw ApiException.forbidden(
        "invitation_wrong_user", "This invitation belongs to another user.");
  }

  private String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String displayName(IdentityUser user) {
    return user.name() == null || user.name().isBlank() ? user.email() : user.name();
  }
}
