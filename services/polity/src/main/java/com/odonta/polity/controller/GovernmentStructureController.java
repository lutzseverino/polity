package com.odonta.polity.controller;

import com.odonta.polity.api.GovernmentStructuresApi;
import com.odonta.polity.api.model.GovernmentStructureResponse;
import com.odonta.polity.mapper.GovernmentStructureTransportMapper;
import com.odonta.polity.service.GovernmentStructureService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${polity.api.base-path}")
@RequiredArgsConstructor
public class GovernmentStructureController implements GovernmentStructuresApi {
  private final GovernmentStructureService government;
  private final GovernmentStructureTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<GovernmentStructureResponse> getPolityGovernment(UUID polityId) {
    return ResponseEntity.ok(mapper.toResponse(government.get(polityId, users.currentUser().id())));
  }
}
