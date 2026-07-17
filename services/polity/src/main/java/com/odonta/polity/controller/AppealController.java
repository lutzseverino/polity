package com.odonta.polity.controller;

import com.odonta.polity.api.AppealsApi;
import com.odonta.polity.api.model.CreateAppealMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.mapper.AppealTransportMapper;
import com.odonta.polity.mapper.MotionTransportMapper;
import com.odonta.polity.service.AppealService;
import com.odonta.polity.workflow.IntroduceAppealMotionWorkflow;
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
public class AppealController implements AppealsApi {
  private final AppealService appeals;
  private final IntroduceAppealMotionWorkflow introduceMotion;
  private final MotionTransportMapper motionsMapper;
  private final AppealTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MotionResponse> createPolityAppealMotion(
      UUID polityId, @Valid CreateAppealMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            motionsMapper.toResponse(
                introduceMotion.introduce(polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<PagedModel> listPolityAppeals(UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        appeals.list(polityId, users.currentUser().id(), page, size), mapper::toResponses);
  }
}
