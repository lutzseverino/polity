package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.MembersApi;
import com.odonta.polity.api.model.AdmitMemberInput;
import com.odonta.polity.api.model.MemberResponse;
import com.odonta.polity.api.model.MembersResponse;
import com.odonta.polity.mapper.PolityMapper;
import com.odonta.polity.service.PolityService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${odonta.api.base-path}")
@RequiredArgsConstructor
public class MemberController implements MembersApi {
  private final PolityService polities;
  private final PolityMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MembersResponse> listPolityMembers(UUID polityId) {
    return ResponseEntity.ok(
        mapper.toMembersResponse(polities.members(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<MemberResponse> admitPolityMember(
      UUID polityId, @Valid AdmitMemberInput input) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.toResponse(polities.admit(polityId, users.currentUser(), input)));
  }
}
