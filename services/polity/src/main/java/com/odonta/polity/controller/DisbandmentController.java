package com.odonta.polity.controller;

import com.odonta.polity.api.DisbandmentsApi;
import com.odonta.polity.api.model.CreateDisbandmentMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.mapper.DisbandmentTransportMapper;
import com.odonta.polity.mapper.MotionTransportMapper;
import com.odonta.polity.workflow.IntroduceDisbandmentMotionWorkflow;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${polity.api.base-path}")
@RequiredArgsConstructor
public class DisbandmentController implements DisbandmentsApi {
  private final IntroduceDisbandmentMotionWorkflow introduceMotion;
  private final MotionTransportMapper motionsMapper;
  private final DisbandmentTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MotionResponse> createPolityDisbandmentMotion(
      UUID polityId, @Valid CreateDisbandmentMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            motionsMapper.toResponse(
                introduceMotion.introduce(polityId, users.currentUser(), mapper.toInput(request))));
  }
}
