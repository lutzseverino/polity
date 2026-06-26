package com.odonta.polity.controller;

import com.odonta.authorization.spring.AuthenticatedUserReader;
import com.odonta.polity.api.JusticeApi;
import com.odonta.polity.api.model.AppealResponse;
import com.odonta.polity.api.model.ConstitutionalReviewResponse;
import com.odonta.polity.api.model.OfficeTermReviewResponse;
import com.odonta.polity.api.model.SanctionResponse;
import com.odonta.polity.mapper.AppealTransportMapper;
import com.odonta.polity.mapper.ConstitutionalReviewTransportMapper;
import com.odonta.polity.mapper.OfficeTermReviewTransportMapper;
import com.odonta.polity.mapper.SanctionTransportMapper;
import com.odonta.polity.service.JusticeService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${odonta.api.base-path}")
@RequiredArgsConstructor
public class JusticeController implements JusticeApi {
  private final JusticeService justice;
  private final SanctionTransportMapper sanctions;
  private final AppealTransportMapper appeals;
  private final OfficeTermReviewTransportMapper officeTermReviews;
  private final ConstitutionalReviewTransportMapper constitutionalReviews;
  private final AuthenticatedUserReader users;

  @Override
  public ResponseEntity<List<SanctionResponse>> listPolitySanctions(UUID polityId) {
    return ResponseEntity.ok(
        sanctions.toResponses(justice.sanctions(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<List<AppealResponse>> listPolityAppeals(UUID polityId) {
    return ResponseEntity.ok(
        appeals.toResponses(justice.appeals(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<List<OfficeTermReviewResponse>> listPolityOfficeTermReviews(UUID polityId) {
    return ResponseEntity.ok(
        officeTermReviews.toResponses(
            justice.officeTermReviews(polityId, users.currentUser().id())));
  }

  @Override
  public ResponseEntity<List<ConstitutionalReviewResponse>> listPolityConstitutionalReviews(
      UUID polityId) {
    return ResponseEntity.ok(
        constitutionalReviews.toResponses(
            justice.constitutionalReviews(polityId, users.currentUser().id())));
  }
}
