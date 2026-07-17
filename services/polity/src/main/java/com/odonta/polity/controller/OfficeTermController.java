package com.odonta.polity.controller;

import com.odonta.polity.api.OfficeTermsApi;
import com.odonta.polity.mapper.OfficeTermTransportMapper;
import com.odonta.polity.service.OfficeTermService;
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
public class OfficeTermController implements OfficeTermsApi {
  private final OfficeTermService officeTerms;
  private final OfficeTermTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<PagedModel> listPolityOfficeTerms(
      UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        officeTerms.list(polityId, users.currentUser().id(), page, size), mapper::toResponses);
  }
}
