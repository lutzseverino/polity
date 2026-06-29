package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.MembersApi;
import com.odonta.polity.api.model.MemberResponse;
import com.odonta.polity.mapper.MembershipTransportMapper;
import com.odonta.polity.service.MembershipResignationService;
import com.odonta.polity.service.MembershipService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${odonta.api.base-path}")
@RequiredArgsConstructor
public class MemberController implements MembersApi {
  private final MembershipService memberships;
  private final MembershipResignationService resignations;
  private final MembershipTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<List<MemberResponse>> listPolityMembers(UUID polityId) {
    return ResponseEntity.ok(
        mapper.toResponses(memberships.list(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<Void> resignPolityMembership(UUID polityId) {
    resignations.resign(polityId, users.currentUser());
    return ResponseEntity.noContent().build();
  }
}
