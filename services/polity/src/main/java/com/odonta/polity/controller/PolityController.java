package com.odonta.polity.controller;

import com.odonta.polity.api.PolitiesApi;
import com.odonta.polity.api.model.CreatePolityRequest;
import com.odonta.polity.api.model.PolityAccountResponse;
import com.odonta.polity.api.model.PolityResponse;
import com.odonta.polity.mapper.PolityAccountTransportMapper;
import com.odonta.polity.mapper.PolityTransportMapper;
import com.odonta.polity.service.PolityAccountService;
import com.odonta.polity.service.PolityService;
import com.odonta.polity.service.PolitySlugLookupService;
import com.odonta.polity.workflow.CreatePolityWorkflow;
import com.odonta.polity.workflow.ProvisionPolityAccountWorkflow;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${polity.api.base-path}")
@RequiredArgsConstructor
public class PolityController implements PolitiesApi {
  private final CreatePolityWorkflow createPolity;
  private final PolityService polities;
  private final PolitySlugLookupService politySlugs;
  private final PolityTransportMapper mapper;
  private final PolityAccountService accounts;
  private final PolityAccountTransportMapper accountMapper;
  private final ProvisionPolityAccountWorkflow provisionAccount;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<PolityAccountResponse> getPolityAccount() {
    return ResponseEntity.ok(accountMapper.toResponse(accounts.get(users.currentUser().id())));
  }

  @Override
  public ResponseEntity<PolityAccountResponse> provisionPolityAccount() {
    var result = provisionAccount.provision(users.currentUser());
    return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(accountMapper.toResponse(result.account()));
  }

  @Override
  public ResponseEntity<PolityResponse> createPolity(@Valid CreatePolityRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.toResponse(createPolity.create(users.currentUser(), mapper.toInput(request))));
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
  public ResponseEntity<PolityResponse> getPolityBySlug(String slug) {
    return ResponseEntity.ok(mapper.toResponse(politySlugs.get(slug, users.currentUser().id())));
  }
}
