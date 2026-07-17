package com.odonta.polity.controller;

import com.odonta.polity.api.OfficeTermReviewsApi;
import com.odonta.polity.api.model.CreateOfficeTermReviewMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.mapper.MotionTransportMapper;
import com.odonta.polity.mapper.OfficeTermReviewTransportMapper;
import com.odonta.polity.service.OfficeTermReviewService;
import com.odonta.polity.workflow.IntroduceOfficeTermReviewMotionWorkflow;
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
public class OfficeTermReviewController implements OfficeTermReviewsApi {
  private final OfficeTermReviewService officeTermReviews;
  private final IntroduceOfficeTermReviewMotionWorkflow introduceMotion;
  private final MotionTransportMapper motionsMapper;
  private final OfficeTermReviewTransportMapper mapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<MotionResponse> createPolityOfficeTermReviewMotion(
      UUID polityId, @Valid CreateOfficeTermReviewMotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            motionsMapper.toResponse(
                introduceMotion.introduce(polityId, users.currentUser(), mapper.toInput(request))));
  }

  @Override
  public ResponseEntity<PagedModel> listPolityOfficeTermReviews(
      UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        officeTermReviews.list(polityId, users.currentUser().id(), page, size),
        mapper::toResponses);
  }
}
