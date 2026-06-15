package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.MotionsApi;
import com.odonta.polity.api.model.CastVoteRequest;
import com.odonta.polity.api.model.CreateMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.mapper.MotionTransportMapper;
import com.odonta.polity.service.MotionService;
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
public class MotionController implements MotionsApi {
  private final MotionService motions;
  private final MotionTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MotionResponse> createPolityMotion(
      UUID polityId, @Valid CreateMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            mapper.toResponse(
                motions.create(polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<List<MotionResponse>> listPolityMotions(UUID polityId) {
    return ResponseEntity.ok(mapper.toResponses(motions.list(polityId, users.currentUser().id())));
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
            motions.vote(polityId, motionId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> certifyPolityMotion(UUID polityId, UUID motionId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.toResponse(motions.certify(polityId, motionId, users.currentUser())));
  }
}
