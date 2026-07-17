package com.odonta.polity.controller;

import com.odonta.polity.api.MembersApi;
import com.odonta.polity.mapper.MembershipTransportMapper;
import com.odonta.polity.service.MembershipService;
import com.odonta.polity.workflow.ResignMembershipWorkflow;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${polity.api.base-path}")
@RequiredArgsConstructor
public class MemberController implements MembersApi {
  private final MembershipService memberships;
  private final ResignMembershipWorkflow resignMembership;
  private final MembershipTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<PagedModel> listPolityMembers(UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        memberships.list(polityId, users.currentUser().id(), page, size), mapper::toResponses);
  }

  @Override
  public ResponseEntity<Void> resignPolityMembership(UUID polityId) {
    resignMembership.resign(polityId, users.currentUser());
    return ResponseEntity.noContent().build();
  }
}
