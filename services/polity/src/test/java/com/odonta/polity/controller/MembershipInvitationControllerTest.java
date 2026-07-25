package com.odonta.polity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.api.model.MembershipInvitationAcceptanceResponse;
import com.odonta.polity.api.model.MembershipInvitationCompletionResponse;
import com.odonta.polity.api.model.MembershipInvitationTokenResponse;
import com.odonta.polity.mapper.MembershipInvitationTransportMapper;
import com.odonta.polity.model.MembershipInvitationAcceptanceStatus;
import com.odonta.polity.result.MembershipInvitationAcceptanceResult;
import com.odonta.polity.result.MembershipInvitationCompletionResult;
import com.odonta.polity.result.MembershipInvitationCompletionStatus;
import com.odonta.polity.result.MembershipInvitationTokenResult;
import com.odonta.polity.service.MembershipInvitationService;
import com.odonta.polity.workflow.AcceptMembershipInvitationWorkflow;
import com.odonta.polity.workflow.CreateMembershipInvitationWorkflow;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipInvitationControllerTest {
  private final MembershipInvitationService invitations = mock(MembershipInvitationService.class);
  private final MembershipInvitationTransportMapper invitationMapper =
      mock(MembershipInvitationTransportMapper.class);
  private final AcceptMembershipInvitationWorkflow acceptance =
      mock(AcceptMembershipInvitationWorkflow.class);
  private final AuthenticatedUserReader users = mock(AuthenticatedUserReader.class);
  private final MembershipInvitationController controller =
      new MembershipInvitationController(
          invitations,
          acceptance,
          mock(CreateMembershipInvitationWorkflow.class),
          invitationMapper,
          users);

  @Test
  void pendingAcceptanceIsReturnedAsAcceptedRequestInsteadOfMember() {
    UUID invitationId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "subject:friend", "Friend");
    OffsetDateTime requestedAt = OffsetDateTime.parse("2026-07-20T10:00:00Z");
    MembershipInvitationAcceptanceResult result =
        new MembershipInvitationAcceptanceResult(
            invitationId, MembershipInvitationAcceptanceStatus.REQUESTED, requestedAt, null, null);
    MembershipInvitationAcceptanceResponse response =
        mock(MembershipInvitationAcceptanceResponse.class);
    when(users.currentUser()).thenReturn(user);
    when(acceptance.accept(invitationId, user)).thenReturn(result);
    when(invitationMapper.toResponse(result)).thenReturn(response);

    var entity = controller.acceptMembershipInvitation(invitationId);

    assertThat(entity.getStatusCode().value()).isEqualTo(202);
    assertThat(entity.getBody()).isSameAs(response);
  }

  @Test
  void secretTokenInspectionIsNeverStoredByIntermediaries() {
    MembershipInvitationTokenResult result =
        new MembershipInvitationTokenResult(
            UUID.randomUUID(),
            "Friend Republic",
            "friend@example.com",
            OffsetDateTime.parse("2026-07-20T10:00:00Z"));
    MembershipInvitationTokenResponse response = mock(MembershipInvitationTokenResponse.class);
    when(invitations.getByToken("secret-token")).thenReturn(result);
    when(invitationMapper.toResponse(result)).thenReturn(response);

    var entity = controller.getMembershipInvitationByToken("secret-token");

    assertThat(entity.getHeaders().getCacheControl()).isEqualTo("no-store");
    assertThat(entity.getBody()).isSameAs(response);
  }

  @Test
  void completionRequestAndPollingAreNeverStoredByIntermediaries() {
    OffsetDateTime now = OffsetDateTime.parse("2026-07-18T10:00:00Z");
    MembershipInvitationCompletionResult result =
        new MembershipInvitationCompletionResult(
            MembershipInvitationCompletionStatus.REQUESTED, 0, null, null, null, now, now);
    MembershipInvitationCompletionResponse response =
        mock(MembershipInvitationCompletionResponse.class);
    when(invitations.requestCompletion("secret-token")).thenReturn(result);
    when(invitations.getCompletion("secret-token")).thenReturn(result);
    when(invitationMapper.toResponse(result)).thenReturn(response);

    var requested = controller.requestMembershipInvitationCompletion("secret-token");
    var polled = controller.getMembershipInvitationCompletion("secret-token");

    assertThat(requested.getStatusCode().value()).isEqualTo(202);
    assertThat(requested.getHeaders().getCacheControl()).isEqualTo("no-store");
    assertThat(polled.getHeaders().getCacheControl()).isEqualTo("no-store");
    assertThat(requested.getBody()).isSameAs(response);
    assertThat(polled.getBody()).isSameAs(response);
  }
}
