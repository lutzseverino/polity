package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.JusticeApi;
import com.odonta.polity.mapper.AppealTransportMapper;
import com.odonta.polity.mapper.ConstitutionalReviewTransportMapper;
import com.odonta.polity.mapper.OfficeTermReviewTransportMapper;
import com.odonta.polity.mapper.SanctionTransportMapper;
import com.odonta.polity.service.AppealService;
import com.odonta.polity.service.ConstitutionalReviewService;
import com.odonta.polity.service.OfficeTermReviewService;
import com.odonta.polity.service.SanctionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${odonta.api.base-path}")
@RequiredArgsConstructor
public class JusticeController implements JusticeApi {
  private final SanctionService sanctionService;
  private final SanctionTransportMapper sanctionMapper;
  private final AppealService appealService;
  private final AppealTransportMapper appealMapper;
  private final OfficeTermReviewService officeTermReviewService;
  private final OfficeTermReviewTransportMapper officeTermReviewMapper;
  private final ConstitutionalReviewService constitutionalReviewService;
  private final ConstitutionalReviewTransportMapper constitutionalReviewMapper;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<PagedModel> listPolitySanctions(UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        sanctionService.list(polityId, users.currentUser().id(), page, size),
        sanctionMapper::toResponses);
  }

  @Override
  public ResponseEntity<PagedModel> listPolityAppeals(UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        appealService.list(polityId, users.currentUser().id(), page, size),
        appealMapper::toResponses);
  }

  @Override
  public ResponseEntity<PagedModel> listPolityOfficeTermReviews(
      UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        officeTermReviewService.list(polityId, users.currentUser().id(), page, size),
        officeTermReviewMapper::toResponses);
  }

  @Override
  public ResponseEntity<PagedModel> listPolityConstitutionalReviews(
      UUID polityId, Integer page, Integer size) {
    return PageResponses.ok(
        constitutionalReviewService.list(polityId, users.currentUser().id(), page, size),
        constitutionalReviewMapper::toResponses);
  }
}
