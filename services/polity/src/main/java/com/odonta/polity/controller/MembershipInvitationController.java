package com.odonta.polity.controller;

import com.odonta.polity.api.MembershipInvitationsApi;
import com.odonta.polity.api.model.CreateMembershipInvitationRequest;
import com.odonta.polity.api.model.MemberResponse;
import com.odonta.polity.api.model.MembershipInvitationCompletionResponse;
import com.odonta.polity.api.model.MembershipInvitationResponse;
import com.odonta.polity.api.model.MembershipInvitationTokenResponse;
import com.odonta.polity.mapper.MembershipInvitationTransportMapper;
import com.odonta.polity.mapper.MembershipTransportMapper;
import com.odonta.polity.service.MembershipInvitationService;
import com.odonta.polity.workflow.AcceptMembershipInvitationWorkflow;
import com.odonta.polity.workflow.CreateMembershipInvitationWorkflow;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${polity.api.base-path}")
@RequiredArgsConstructor
public class MembershipInvitationController implements MembershipInvitationsApi {
  private final MembershipInvitationService invitations;
  private final AcceptMembershipInvitationWorkflow acceptance;
  private final CreateMembershipInvitationWorkflow creation;
  private final MembershipInvitationTransportMapper invitationMapper;
  private final MembershipTransportMapper memberMapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MembershipInvitationResponse> createPolityMembershipInvitation(
      UUID polityId, @Valid CreateMembershipInvitationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            invitationMapper.toResponse(
                creation.create(polityId, users.currentUser(), invitationMapper.toInput(request))));
  }

  @Override
  public ResponseEntity<PagedModel> listPolityMembershipInvitations(
      UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        invitations.listPolityMembershipInvitations(polityId, users.currentUser().id(), page, size),
        invitationMapper::toResponses);
  }

  @Override
  public ResponseEntity<PagedModel> listCurrentUserMembershipInvitations(
      Integer page, Integer size) {
    return PageResponses.ok(
        invitations.listCurrentUserMembershipInvitations(users.currentUser(), page, size),
        invitationMapper::toResponses);
  }

  @Override
  public ResponseEntity<MemberResponse> acceptMembershipInvitation(UUID invitationId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(memberMapper.toResponse(acceptance.accept(invitationId, users.currentUser())));
  }

  @Override
  public ResponseEntity<MembershipInvitationTokenResponse> getMembershipInvitationByToken(
      String token) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(invitationMapper.toResponse(invitations.getByToken(token)));
  }

  @Override
  public ResponseEntity<MembershipInvitationCompletionResponse>
      requestMembershipInvitationCompletion(String token) {
    return ResponseEntity.accepted()
        .cacheControl(CacheControl.noStore())
        .body(invitationMapper.toResponse(invitations.requestCompletion(token)));
  }

  @Override
  public ResponseEntity<MembershipInvitationCompletionResponse> getMembershipInvitationCompletion(
      String token) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(invitationMapper.toResponse(invitations.getCompletion(token)));
  }
}
