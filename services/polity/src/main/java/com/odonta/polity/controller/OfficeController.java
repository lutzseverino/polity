package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.OfficesApi;
import com.odonta.polity.api.model.OfficeResponse;
import com.odonta.polity.api.model.OfficeTermResponse;
import com.odonta.polity.mapper.OfficeTermTransportMapper;
import com.odonta.polity.mapper.OfficeTransportMapper;
import com.odonta.polity.service.OfficeService;
import com.odonta.polity.service.OfficeTermService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${odonta.api.base-path}")
@RequiredArgsConstructor
public class OfficeController implements OfficesApi {
  private final OfficeTransportMapper officeMapper;
  private final OfficeTermTransportMapper officeTermMapper;
  private final OfficeService offices;
  private final OfficeTermService officeTerms;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<List<OfficeResponse>> listPolityOffices(
      UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        offices.list(polityId, users.currentUser().id(), page, size), officeMapper::toResponses);
  }

  @Override
  public ResponseEntity<List<OfficeTermResponse>> listPolityOfficeTerms(
      UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        officeTerms.list(polityId, users.currentUser().id(), page, size),
        officeTermMapper::toResponses);
  }
}
