package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.MembersApi;
import com.odonta.polity.api.model.MemberResponse;
import com.odonta.polity.mapper.PolityTransportMapper;
import com.odonta.polity.service.PolityService;
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
  private final PolityService polities;
  private final PolityTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<List<MemberResponse>> listPolityMembers(UUID polityId) {
    return ResponseEntity.ok(
        mapper.toMemberResponses(polities.members(polityId, users.currentUser().id())));
  }
}
