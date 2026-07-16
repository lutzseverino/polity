package com.odonta.polity.controller;

import com.odonta.polity.api.OfficialRecordApi;
import com.odonta.polity.mapper.OfficialRecordTransportMapper;
import com.odonta.polity.service.OfficialRecordService;
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
public class OfficialRecordController implements OfficialRecordApi {
  private final OfficialRecordService records;
  private final OfficialRecordTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<PagedModel> listPolityOfficialRecord(
      UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        records.list(polityId, users.currentUser().id(), page, size), mapper::toResponses);
  }
}
