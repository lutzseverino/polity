package com.odonta.polity.controller;

import com.odonta.polity.api.PolityActionAvailabilityApi;
import com.odonta.polity.api.model.PolityActionAvailabilityResponse;
import com.odonta.polity.mapper.PolityActionAvailabilityTransportMapper;
import com.odonta.polity.service.PolityActionAvailabilityService;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${polity.api.base-path}")
@RequiredArgsConstructor
public class PolityActionAvailabilityController implements PolityActionAvailabilityApi {
  private final PolityActionAvailabilityService actionAvailability;
  private final PolityActionAvailabilityTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<PolityActionAvailabilityResponse> getPolityActions(UUID polityId) {
    return ResponseEntity.ok(
        mapper.toResponse(actionAvailability.get(polityId, users.currentUser().id())));
  }
}
