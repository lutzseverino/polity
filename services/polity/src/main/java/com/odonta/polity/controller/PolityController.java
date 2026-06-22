package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.PolitiesApi;
import com.odonta.polity.api.model.ConstitutionResponse;
import com.odonta.polity.api.model.CreatePolityRequest;
import com.odonta.polity.api.model.PolityActionAvailabilityResponse;
import com.odonta.polity.api.model.PolityResponse;
import com.odonta.polity.mapper.PolityTransportMapper;
import com.odonta.polity.service.PolityService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${odonta.api.base-path}")
@RequiredArgsConstructor
public class PolityController implements PolitiesApi {
  private final PolityService polities;
  private final PolityTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<PolityResponse> createPolity(@Valid CreatePolityRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.toResponse(polities.create(users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<List<PolityResponse>> listPolities() {
    return ResponseEntity.ok(mapper.toResponses(polities.list(users.currentUser().id())));
  }

  @Override
  public ResponseEntity<PolityResponse> getPolity(UUID polityId) {
    return ResponseEntity.ok(mapper.toResponse(polities.get(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<ConstitutionResponse> getPolityConstitution(UUID polityId) {
    return ResponseEntity.ok(
        mapper.toResponse(polities.getConstitution(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<PolityActionAvailabilityResponse> getPolityActions(UUID polityId) {
    return ResponseEntity.ok(
        mapper.toResponse(polities.getActionAvailability(polityId, users.currentUser().id())));
  }
}
