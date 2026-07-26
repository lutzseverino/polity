package com.odonta.polity.workflow;

import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipInvitation;
import com.odonta.polity.model.MembershipInvitationAcceptanceStatus;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.service.OfficialRecordService;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceipt;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import io.github.lutzseverino.cardo.identity.client.IdentityUser;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CompleteMembershipInvitationWorkflow {
  private final Clock clock;
  private final Grants grants;
  private final MembershipInvitationRepository invitations;
  private final MembershipRepository memberships;
  private final OfficialRecordService officialRecords;
  private final PolityGrantPlanner grantPlanner;
  private final PolityBootstrapCompleter bootstrap;
  private final PolityContextResolver polityContext;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void complete(UUID invitationId, IdentityUser identity, OffsetDateTime acceptedAt) {
    MembershipInvitation invitation =
        invitations
            .findEntityByIdForUpdate(invitationId)
            .orElseThrow(() -> new IllegalStateException("Membership invitation not found."));
    if (invitation.getAcceptanceStatus() == MembershipInvitationAcceptanceStatus.COMPLETED
        || invitation.getAcceptanceStatus() == MembershipInvitationAcceptanceStatus.FAILED) {
      return;
    }
    if (invitation.getAcceptanceStatus() != MembershipInvitationAcceptanceStatus.REQUESTED) {
      throw new IllegalStateException("Membership invitation acceptance was not requested.");
    }
    requireInvitee(invitation, identity);
    Membership admitted = admit(invitation, identity, acceptedAt);
    if (admitted == null) {
      return;
    }
    invitation.completeAcceptance(acceptedAt, OffsetDateTime.now(clock));
    invitations.saveAndFlush(invitation);
    bootstrap.completeIfReady(invitation.getPolityId(), acceptedAt);
    recordAdmission(invitation, admitted, acceptedAt);
  }

  private Membership admit(
      MembershipInvitation invitation, IdentityUser identity, OffsetDateTime acceptedAt) {
    Membership existing =
        memberships
            .findEntityByPolityIdAndUserIdForUpdate(invitation.getPolityId(), identity.id())
            .orElse(null);
    if (existing != null && existing.getStatus() == MembershipStatus.ACTIVE) {
      invitation.failAcceptance("member_exists", OffsetDateTime.now(clock));
      invitations.saveAndFlush(invitation);
      return null;
    }
    GrantReceipt receipt =
        grants.stage(
            grantPlanner.membership(identity.authorizationSubject(), invitation.getPolityId()));
    if (existing != null) {
      existing.reactivate(
          identity.authorizationSubject(),
          identity.email(),
          displayName(identity),
          acceptedAt,
          invitation.getInvitedBy(),
          receipt.id());
      return memberships.saveAndFlush(existing);
    }
    Membership membership =
        new Membership(
            invitation.getPolityId(),
            identity.id(),
            identity.authorizationSubject(),
            identity.email(),
            displayName(identity),
            acceptedAt,
            invitation.getInvitedBy());
    membership.retainGrantReceipt(receipt.id());
    return memberships.saveAndFlush(membership);
  }

  private void requireInvitee(MembershipInvitation invitation, IdentityUser identity) {
    if (invitation.getInvitedUserId().equals(identity.id())
        || invitation.getEmail().equals(identity.email().trim().toLowerCase(Locale.ROOT))) {
      return;
    }
    throw new IllegalStateException("Cardo identity no longer matches the membership invitation.");
  }

  private String displayName(IdentityUser user) {
    return user.name() == null || user.name().isBlank() ? user.email() : user.name();
  }

  private void recordAdmission(
      MembershipInvitation invitation, Membership admitted, OffsetDateTime admittedAt) {
    var constitution = polityContext.constitution(invitation.getPolityId());
    Jurisdiction jurisdiction = polityContext.rootJurisdiction(invitation.getPolityId());
    officialRecords.append(
        invitation.getPolityId(),
        jurisdiction.getId(),
        constitution.getId(),
        admitted.getId(),
        OfficialRecordType.MEMBER_ADMITTED,
        admitted.getId(),
        OfficialRecordContext.none(),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.MEMBER_ADMITTED,
            TemplateParameters.of("memberName", admitted.getDisplayName())),
        admittedAt);
  }
}
