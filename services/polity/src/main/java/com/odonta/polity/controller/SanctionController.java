package com.odonta.polity.controller;

import com.odonta.polity.api.SanctionsApi;
import com.odonta.polity.api.model.CreateSanctionMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.mapper.MotionTransportMapper;
import com.odonta.polity.mapper.SanctionTransportMapper;
import com.odonta.polity.service.SanctionService;
import com.odonta.polity.workflow.IntroduceSanctionMotionWorkflow;
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
public class SanctionController implements SanctionsApi {
  private final SanctionService sanctions;
  private final IntroduceSanctionMotionWorkflow introduceMotion;
  private final MotionTransportMapper motionsMapper;
  private final SanctionTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MotionResponse> createPolitySanctionMotion(
      UUID polityId, @Valid CreateSanctionMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            motionsMapper.toResponse(
                introduceMotion.introduce(polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<PagedModel> listPolitySanctions(UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        sanctions.list(polityId, users.currentUser().id(), page, size), mapper::toResponses);
  }
}
