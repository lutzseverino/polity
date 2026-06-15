package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.OfficialRecordApi;
import com.odonta.polity.api.model.OfficialRecordEntryResponse;
import com.odonta.polity.mapper.OfficialRecordTransportMapper;
import com.odonta.polity.service.OfficialRecordService;
import java.util.List;
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
  private final OfficialRecordTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<List<OfficialRecordEntryResponse>> listPolityOfficialRecord(UUID polityId) {
    return ResponseEntity.ok(mapper.toResponses(records.list(polityId, users.currentUser().id())));
  }
}
