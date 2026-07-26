package com.odonta.polity.integration.invite;

import com.odonta.polity.config.MembershipInvitationProperties;
import com.odonta.polity.repository.MembershipInvitationProjection;
import com.odonta.polity.repository.MembershipInvitationRepository;
import com.odonta.polity.workflow.CompleteMembershipInvitationWorkflow;
import io.github.lutzseverino.cardo.common.api.ApiException;
import io.github.lutzseverino.cardo.identity.client.IdentityUser;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import io.github.lutzseverino.cardo.invite.client.CreateInvitation;
import io.github.lutzseverino.cardo.invite.client.CreatedInvitation;
import io.github.lutzseverino.cardo.invite.client.Invitation;
import io.github.lutzseverino.cardo.invite.client.InvitationStatus;
import io.github.lutzseverino.cardo.invite.client.InvitationsClient;
import java.util.UUID;

class CardoInvitationProcessor {
  private final InvitationsClient client;
  private final IdentityUsersClient identityUsers;
  private final MembershipInvitationRepository invitations;
  private final MembershipInvitationProperties properties;
  private final CardoInvitationState state;
  private final CompleteMembershipInvitationWorkflow completion;

  CardoInvitationProcessor(
      InvitationsClient client,
      IdentityUsersClient identityUsers,
      MembershipInvitationRepository invitations,
      MembershipInvitationProperties properties,
      CardoInvitationState state,
      CompleteMembershipInvitationWorkflow completion) {
    this.client = client;
    this.identityUsers = identityUsers;
    this.invitations = invitations;
    this.properties = properties;
    this.state = state;
    this.completion = completion;
  }

  void create(CardoInvitationCreationRequested request) {
    MembershipInvitationProjection invitation = requireInvitation(request.invitationId());
    CreateInvitation input =
        new CreateInvitation(
            invitation.getId(),
            invitation.getPolityId(),
            com.odonta.polity.PolityPermissions.POLITY_RESOURCE,
            invitation.getEmail(),
            request.invitedByUserId(),
            properties.acceptUrlBase());
    CreatedInvitation created = client.create(input);
    requireMatchingCreation(input, created.invitation());
    state.register(
        invitation.getId(),
        created.invitation().id(),
        created.invitation().invitedUserId(),
        created.invitation().expiresAt());
  }

  private void requireMatchingCreation(CreateInvitation requested, Invitation created) {
    if (!requested.requestId().equals(created.requestId())
        || !requested.tenantId().equals(created.tenantId())
        || !requested.tenantResourceType().equals(created.tenantResourceType())
        || !requested.email().equalsIgnoreCase(created.invitedEmail())
        || !requested.invitedBy().equals(created.invitedBy())
        || !InvitationStatus.PENDING.equals(created.status())) {
      throw new IllegalStateException(
          "Cardo invitation creation response does not match the requested invitation.");
    }
  }

  void accept(CardoInvitationAcceptanceRequested request) {
    MembershipInvitationProjection invitation = requireInvitation(request.invitationId());
    if (invitation.getCardoInvitationId() == null) {
      throw new IllegalStateException("Cardo invitation has not been created yet.");
    }
    Invitation accepted;
    try {
      accepted = client.accept(invitation.getCardoInvitationId(), request.acceptedAt());
    } catch (ApiException exception) {
      if (isTerminalAcceptanceFailure(exception)) {
        state.fail(request.invitationId(), exception.code());
        return;
      }
      throw exception;
    }
    requireMatchingAcceptance(invitation, request, accepted);
    IdentityUser identity = identityUsers.get(invitation.getInvitedUserId());
    completion.complete(request.invitationId(), identity, request.acceptedAt());
  }

  private boolean isTerminalAcceptanceFailure(ApiException exception) {
    return (exception.status() == 409 && exception.code().equals("invitation_revoked"))
        || (exception.status() == 410
            && (exception.code().equals("invitation_unavailable")
                || exception.code().equals("invitation_expired")));
  }

  private void requireMatchingAcceptance(
      MembershipInvitationProjection invitation,
      CardoInvitationAcceptanceRequested request,
      Invitation accepted) {
    if (!invitation.getCardoInvitationId().equals(accepted.id())
        || !invitation.getId().equals(accepted.requestId())
        || !InvitationStatus.ACCEPTED.equals(accepted.status())
        || !request.acceptedAt().isEqual(accepted.acceptedAt())) {
      throw new IllegalStateException(
          "Cardo invitation acceptance response does not match the requested acceptance.");
    }
  }

  private MembershipInvitationProjection requireInvitation(UUID invitationId) {
    return invitations
        .findProjectedById(invitationId)
        .orElseThrow(() -> new IllegalStateException("Membership invitation not found."));
  }
}
