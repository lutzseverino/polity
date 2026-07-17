package com.odonta.polity.controller;

import com.odonta.polity.api.OfficeElectionsApi;
import com.odonta.polity.api.model.CastOfficeElectionBallotRequest;
import com.odonta.polity.api.model.CreateOfficeElectionMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.api.model.RespondOfficeElectionCandidacyRequest;
import com.odonta.polity.mapper.MotionTransportMapper;
import com.odonta.polity.mapper.OfficeElectionTransportMapper;
import com.odonta.polity.workflow.CastOfficeElectionBallotWorkflow;
import com.odonta.polity.workflow.IntroduceOfficeElectionMotionWorkflow;
import com.odonta.polity.workflow.RespondOfficeElectionCandidacyWorkflow;
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
public class OfficeElectionController implements OfficeElectionsApi {
  private final CastOfficeElectionBallotWorkflow castBallot;
  private final IntroduceOfficeElectionMotionWorkflow introduceMotion;
  private final MotionTransportMapper motionsMapper;
  private final OfficeElectionTransportMapper mapper;
  private final RespondOfficeElectionCandidacyWorkflow respondCandidacy;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MotionResponse> createPolityOfficeElectionMotion(
      UUID polityId, @Valid CreateOfficeElectionMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            motionsMapper.toResponse(
                introduceMotion.introduce(polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> castPolityOfficeElectionBallot(
      UUID polityId, UUID motionId, @Valid CastOfficeElectionBallotRequest request) {
    return ResponseEntity.ok(
        motionsMapper.toResponse(
            castBallot.cast(polityId, motionId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> respondPolityOfficeElectionCandidacy(
      UUID polityId, UUID motionId, @Valid RespondOfficeElectionCandidacyRequest request) {
    return ResponseEntity.ok(
        motionsMapper.toResponse(
            respondCandidacy.respond(
                polityId, motionId, users.currentUser(), mapper.toInput(request))));
  }
}
