package com.odonta.polity.controller;

import com.odonta.polity.api.ConstitutionsApi;
import com.odonta.polity.api.model.ConstitutionResponse;
import com.odonta.polity.mapper.ConstitutionTransportMapper;
import com.odonta.polity.service.ConstitutionService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${polity.api.base-path}")
@RequiredArgsConstructor
public class ConstitutionController implements ConstitutionsApi {
  private final ConstitutionService constitutions;
  private final ConstitutionTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<ConstitutionResponse> getPolityConstitution(UUID polityId) {
    return ResponseEntity.ok(
        mapper.toResponse(constitutions.get(polityId, users.currentUser().id())));
  }
}
