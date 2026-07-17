package com.odonta.polity.controller;

import com.odonta.polity.api.MotionsApi;
import com.odonta.polity.api.model.CastVoteRequest;
import com.odonta.polity.api.model.CreateMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.mapper.MotionTransportMapper;
import com.odonta.polity.mapper.MotionVotingTransportMapper;
import com.odonta.polity.service.MotionService;
import com.odonta.polity.workflow.CastMotionVoteWorkflow;
import com.odonta.polity.workflow.CertifyMotionWorkflow;
import com.odonta.polity.workflow.IntroduceMotionWorkflow;
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
public class MotionController implements MotionsApi {
  private final CastMotionVoteWorkflow castMotionVote;
  private final CertifyMotionWorkflow certifyMotion;
  private final MotionTransportMapper mapper;
  private final MotionService motions;
  private final IntroduceMotionWorkflow introduceMotion;
  private final MotionVotingTransportMapper motionVotingMapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MotionResponse> createPolityMotion(
      UUID polityId, @Valid CreateMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            mapper.toResponse(
                introduceMotion.introduce(polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<PagedModel> listPolityMotions(UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        motions.list(polityId, users.currentUser().id(), page, size), mapper::toResponses);
  }

  @Override
  public ResponseEntity<MotionResponse> getPolityMotion(UUID polityId, UUID motionId) {
    return ResponseEntity.ok(
        mapper.toResponse(motions.get(polityId, motionId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<MotionResponse> castPolityMotionVote(
      UUID polityId, UUID motionId, @Valid CastVoteRequest request) {
    return ResponseEntity.ok(
        mapper.toResponse(
            castMotionVote.cast(
                polityId, motionId, users.currentUser(), motionVotingMapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> certifyPolityMotion(UUID polityId, UUID motionId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.toResponse(certifyMotion.certify(polityId, motionId, users.currentUser())));
  }
}
