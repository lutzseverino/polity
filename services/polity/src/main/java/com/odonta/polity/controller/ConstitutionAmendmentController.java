package com.odonta.polity.controller;

import com.odonta.polity.api.ConstitutionAmendmentsApi;
import com.odonta.polity.api.model.CreateConstitutionAmendmentMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.mapper.ConstitutionAmendmentTransportMapper;
import com.odonta.polity.mapper.MotionTransportMapper;
import com.odonta.polity.workflow.IntroduceConstitutionAmendmentMotionWorkflow;
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
public class ConstitutionAmendmentController implements ConstitutionAmendmentsApi {
  private final IntroduceConstitutionAmendmentMotionWorkflow introduceMotion;
  private final MotionTransportMapper motionsMapper;
  private final ConstitutionAmendmentTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MotionResponse> createPolityConstitutionAmendmentMotion(
      UUID polityId, @Valid CreateConstitutionAmendmentMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            motionsMapper.toResponse(
                introduceMotion.introduce(polityId, users.currentUser(), mapper.toInput(request))));
  }
}
