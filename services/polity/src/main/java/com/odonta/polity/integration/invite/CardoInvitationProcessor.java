package com.odonta.polity.integration.invite;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.config.MembershipInvitationProperties;
import com.odonta.polity.repository.MembershipInvitationProjection;
import com.odonta.polity.repository.MembershipInvitationRepository;
import io.github.lutzseverino.cardo.invite.client.CreateInvitation;
import io.github.lutzseverino.cardo.invite.client.CreatedInvitation;
import io.github.lutzseverino.cardo.invite.client.Invitation;
import io.github.lutzseverino.cardo.invite.client.InvitationGrant;
import io.github.lutzseverino.cardo.invite.client.InvitationStatus;
import io.github.lutzseverino.cardo.invite.client.InvitationsClient;
import java.util.List;
import java.util.UUID;

class CardoInvitationProcessor {
  private static final String MEMBER_ACCESS_PROFILE = "polity:member";
  private static final List<InvitationGrant> MEMBER_GRANTS =
      List.of(
          new InvitationGrant(PolityPermissions.POLITY_RESOURCE, PolityPermissions.READ),
          new InvitationGrant(PolityPermissions.POLITY_RESOURCE, PolityPermissions.PARTICIPATE));

  private final InvitationsClient client;
  private final MembershipInvitationRepository invitations;
  private final MembershipInvitationProperties properties;
  private final CardoInvitationState state;

  CardoInvitationProcessor(
      InvitationsClient client,
      MembershipInvitationRepository invitations,
      MembershipInvitationProperties properties,
      CardoInvitationState state) {
    this.client = client;
    this.invitations = invitations;
    this.properties = properties;
    this.state = state;
  }

  void create(CardoInvitationCreationRequested request) {
    MembershipInvitationProjection invitation = requireInvitation(request.invitationId());
    CreateInvitation input =
        new CreateInvitation(
            invitation.getId(),
            invitation.getPolityId(),
            PolityPermissions.POLITY_RESOURCE,
            invitation.getEmail(),
            MEMBER_ACCESS_PROFILE,
            MEMBER_GRANTS,
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
        || !requested.accessProfile().equals(created.accessProfile())
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
    client.accept(invitation.getCardoInvitationId(), request.acceptedAt());
  }

  private MembershipInvitationProjection requireInvitation(UUID invitationId) {
    return invitations
        .findProjectedById(invitationId)
        .orElseThrow(() -> new IllegalStateException("Membership invitation not found."));
  }
}
