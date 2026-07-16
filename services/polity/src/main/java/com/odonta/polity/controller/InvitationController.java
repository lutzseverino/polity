package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.InvitationsApi;
import com.odonta.polity.api.model.CreateMemberInvitationRequest;
import com.odonta.polity.api.model.MemberInvitationResponse;
import com.odonta.polity.api.model.MemberResponse;
import com.odonta.polity.mapper.MembershipInvitationTransportMapper;
import com.odonta.polity.mapper.MembershipTransportMapper;
import com.odonta.polity.service.InvitationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${odonta.api.base-path}")
@RequiredArgsConstructor
public class InvitationController implements InvitationsApi {
  private final InvitationService invitations;
  private final MembershipInvitationTransportMapper invitationMapper;
  private final MembershipTransportMapper memberMapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MemberInvitationResponse> createPolityInvitation(
      UUID polityId, @Valid CreateMemberInvitationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            invitationMapper.toResponse(
                invitations.create(
                    polityId, users.currentUser(), invitationMapper.toInput(request))));
  }

  @Override
  public ResponseEntity<PagedModel> listPolityInvitations(
      UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        invitations.listPolityInvitations(polityId, users.currentUser().id(), page, size),
        invitationMapper::toResponses);
  }

  @Override
  public ResponseEntity<PagedModel> listCurrentUserInvitations(Integer page, Integer size) {
    return PageResponses.ok(
        invitations.listCurrentUserInvitations(users.currentUser(), page, size),
        invitationMapper::toResponses);
  }

  @Override
  public ResponseEntity<MemberResponse> acceptInvitation(UUID invitationId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(memberMapper.toResponse(invitations.accept(invitationId, users.currentUser())));
  }
}
