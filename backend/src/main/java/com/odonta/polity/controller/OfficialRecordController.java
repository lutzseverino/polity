package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.OfficialRecordApi;
import com.odonta.polity.api.model.OfficialRecordResponse;
import com.odonta.polity.mapper.OfficialRecordMapper;
import com.odonta.polity.service.OfficialRecordService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${odonta.api.base-path}")
@RequiredArgsConstructor
public class OfficialRecordController implements OfficialRecordApi {
  private final OfficialRecordService records;
  private final OfficialRecordMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<OfficialRecordResponse> listPolityOfficialRecord(UUID polityId) {
    return ResponseEntity.ok(mapper.toResponse(records.list(polityId, users.currentUser().id())));
  }
}
