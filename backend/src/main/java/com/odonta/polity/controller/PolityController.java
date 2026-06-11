package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.PolitiesApi;
import com.odonta.polity.api.model.CreatePolityRequest;
import com.odonta.polity.api.model.PolitiesResponse;
import com.odonta.polity.api.model.PolityResponse;
import com.odonta.polity.mapper.PolityMapper;
import com.odonta.polity.model.CreatePolityCommand;
import com.odonta.polity.service.PolityService;
import jakarta.validation.Valid;
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
  private final PolityMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<PolityResponse> createPolity(@Valid CreatePolityRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            mapper.toResponse(
                polities.create(users.currentUser(), new CreatePolityCommand(request.getName()))));
  }

  @Override
  public ResponseEntity<PolitiesResponse> listPolities() {
    return ResponseEntity.ok(mapper.toPolitiesResponse(polities.list(users.currentUser().id())));
  }

  @Override
  public ResponseEntity<PolityResponse> getPolity(UUID polityId) {
    return ResponseEntity.ok(mapper.toResponse(polities.get(polityId, users.currentUser().id())));
  }
}
