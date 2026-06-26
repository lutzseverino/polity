package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.MotionsApi;
import com.odonta.polity.api.model.CastOfficeElectionBallotRequest;
import com.odonta.polity.api.model.CastVoteRequest;
import com.odonta.polity.api.model.CreateAppealMotionRequest;
import com.odonta.polity.api.model.CreateConstitutionAmendmentMotionRequest;
import com.odonta.polity.api.model.CreateConstitutionalReviewMotionRequest;
import com.odonta.polity.api.model.CreateDisbandmentMotionRequest;
import com.odonta.polity.api.model.CreateMotionRequest;
import com.odonta.polity.api.model.CreateOfficeElectionMotionRequest;
import com.odonta.polity.api.model.CreateOfficeTermReviewMotionRequest;
import com.odonta.polity.api.model.CreateSanctionMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.api.model.RespondOfficeElectionCandidacyRequest;
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
  public ResponseEntity<MotionResponse> createPolityOfficeElectionMotion(
      UUID polityId, @Valid CreateOfficeElectionMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            mapper.toResponse(
                motions.createOfficeElection(
                    polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> createPolitySanctionMotion(
      UUID polityId, @Valid CreateSanctionMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            mapper.toResponse(
                motions.createSanction(polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> createPolityAppealMotion(
      UUID polityId, @Valid CreateAppealMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            mapper.toResponse(
                motions.createAppeal(polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> createPolityOfficeTermReviewMotion(
      UUID polityId, @Valid CreateOfficeTermReviewMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            mapper.toResponse(
                motions.createOfficeTermReview(
                    polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> createPolityConstitutionalReviewMotion(
      UUID polityId, @Valid CreateConstitutionalReviewMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            mapper.toResponse(
                motions.createConstitutionalReview(
                    polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> createPolityConstitutionAmendmentMotion(
      UUID polityId, @Valid CreateConstitutionAmendmentMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            mapper.toResponse(
                motions.createAmendment(polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> createPolityDisbandmentMotion(
      UUID polityId, @Valid CreateDisbandmentMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            mapper.toResponse(
                motions.createDisbandment(polityId, users.currentUser(), mapper.toInput(request))));
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
  public ResponseEntity<MotionResponse> castPolityOfficeElectionBallot(
      UUID polityId, UUID motionId, @Valid CastOfficeElectionBallotRequest request) {
    return ResponseEntity.ok(
        mapper.toResponse(
            motions.castOfficeElectionBallot(
                polityId, motionId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> respondPolityOfficeElectionCandidacy(
      UUID polityId, UUID motionId, @Valid RespondOfficeElectionCandidacyRequest request) {
    return ResponseEntity.ok(
        mapper.toResponse(
            motions.respondOfficeElectionCandidacy(
                polityId, motionId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<MotionResponse> certifyPolityMotion(UUID polityId, UUID motionId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.toResponse(motions.certify(polityId, motionId, users.currentUser())));
  }
}
