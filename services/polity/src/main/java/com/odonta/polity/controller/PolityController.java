package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.PolitiesApi;
import com.odonta.polity.api.model.ConstitutionResponse;
import com.odonta.polity.api.model.CreatePolityRequest;
import com.odonta.polity.api.model.GovernmentStructureResponse;
import com.odonta.polity.api.model.PolityActionAvailabilityResponse;
import com.odonta.polity.api.model.PolityResponse;
import com.odonta.polity.mapper.ConstitutionTransportMapper;
import com.odonta.polity.mapper.GovernmentStructureTransportMapper;
import com.odonta.polity.mapper.PolityActionAvailabilityTransportMapper;
import com.odonta.polity.mapper.PolityTransportMapper;
import com.odonta.polity.resolver.GovernmentStructureResolver;
import com.odonta.polity.resolver.PolityActionAvailabilityResolver;
import com.odonta.polity.service.ConstitutionService;
import com.odonta.polity.service.PolityService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
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
  private final ConstitutionService constitutions;
  private final ConstitutionTransportMapper constitutionMapper;
  private final GovernmentStructureResolver government;
  private final GovernmentStructureTransportMapper governmentMapper;
  private final PolityActionAvailabilityTransportMapper actionMapper;
  private final PolityActionAvailabilityResolver actionAvailability;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<Void> provisionPolityAccount() {
    polities.provisionAccount(users.currentUser());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<PolityResponse> createPolity(@Valid CreatePolityRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.toResponse(polities.create(users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<PagedModel> listPolities(String query, Integer page, Integer size) {
    return PageResponses.ok(
        polities.list(users.currentUser().id(), query, page, size), mapper::toResponses);
  }

  @Override
  public ResponseEntity<PolityResponse> getPolity(UUID polityId) {
    return ResponseEntity.ok(mapper.toResponse(polities.get(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<ConstitutionResponse> getPolityConstitution(UUID polityId) {
    return ResponseEntity.ok(
        constitutionMapper.toResponse(constitutions.get(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<GovernmentStructureResponse> getPolityGovernment(UUID polityId) {
    return ResponseEntity.ok(
        governmentMapper.toResponse(government.resolve(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<PolityActionAvailabilityResponse> getPolityActions(UUID polityId) {
    return ResponseEntity.ok(
        actionMapper.toResponse(actionAvailability.resolve(polityId, users.currentUser().id())));
  }
}
