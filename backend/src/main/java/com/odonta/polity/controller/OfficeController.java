package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.OfficesApi;
import com.odonta.polity.api.model.OfficeResponse;
import com.odonta.polity.api.model.OfficeTermResponse;
import com.odonta.polity.mapper.OfficeTransportMapper;
import com.odonta.polity.service.OfficeService;
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
  private final OfficeTransportMapper mapper;
  private final OfficeService offices;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<List<OfficeResponse>> listPolityOffices(UUID polityId) {
    return ResponseEntity.ok(mapper.toResponses(offices.list(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<List<OfficeTermResponse>> listPolityOfficeTerms(UUID polityId) {
    return ResponseEntity.ok(
        mapper.toTermResponses(offices.terms(polityId, users.currentUser().id())));
  }
}
