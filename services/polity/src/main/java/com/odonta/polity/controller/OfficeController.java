package com.odonta.polity.controller;

import com.odonta.polity.api.OfficesApi;
import com.odonta.polity.mapper.OfficeTransportMapper;
import com.odonta.polity.service.OfficeService;
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
public class OfficeController implements OfficesApi {
  private final OfficeService offices;
  private final OfficeTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<PagedModel> listPolityOffices(UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        offices.list(polityId, users.currentUser().id(), page, size), mapper::toResponses);
  }
}
