package com.odonta.polity.controller;

import com.odonta.polity.api.ConstitutionalReviewsApi;
import com.odonta.polity.api.model.CreateConstitutionalReviewMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.mapper.ConstitutionalReviewTransportMapper;
import com.odonta.polity.mapper.MotionTransportMapper;
import com.odonta.polity.service.ConstitutionalReviewService;
import com.odonta.polity.workflow.IntroduceConstitutionalReviewMotionWorkflow;
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
public class ConstitutionalReviewController implements ConstitutionalReviewsApi {
  private final ConstitutionalReviewService constitutionalReviews;
  private final IntroduceConstitutionalReviewMotionWorkflow introduceMotion;
  private final MotionTransportMapper motionsMapper;
  private final ConstitutionalReviewTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MotionResponse> createPolityConstitutionalReviewMotion(
      UUID polityId, @Valid CreateConstitutionalReviewMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            motionsMapper.toResponse(
                introduceMotion.introduce(polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<PagedModel> listPolityConstitutionalReviews(
      UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        constitutionalReviews.list(polityId, users.currentUser().id(), page, size),
        mapper::toResponses);
  }
}
