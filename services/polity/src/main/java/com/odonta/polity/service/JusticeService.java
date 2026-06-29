package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.JusticeApplicationMapper;
import com.odonta.polity.model.AppealResult;
import com.odonta.polity.model.ConstitutionalReviewResult;
import com.odonta.polity.model.OfficeTermReviewResult;
import com.odonta.polity.model.SanctionResult;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.repository.AppealProjection;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionalReviewProjection;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProjection;
import com.odonta.polity.repository.OfficeTermReviewRepository;
import com.odonta.polity.repository.OfficialRecordProjection;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.SanctionProjection;
import com.odonta.polity.repository.SanctionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JusticeService {
  private final PolityAccessPolicy access;
  private final Clock clock;
  private final AppealRepository appeals;
  private final ConstitutionalReviewRepository constitutionalReviews;
  private final JusticeApplicationMapper mapper;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficeTermReviewRepository officeTermReviews;
  private final OfficialRecordRepository officialRecords;
  private final MembershipService memberships;
  private final SanctionRepository sanctions;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<SanctionResult> sanctions(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    OffsetDateTime now = OffsetDateTime.now(clock);
    return sanctions.findProjectionsByPolityIdOrderByStartedAtDesc(polityId).stream()
        .map(sanction -> sanction(sanction, now))
        .toList();
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<AppealResult> appeals(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return appeals.findProjectionsByPolityIdOrderByDecidedAtDesc(polityId).stream()
        .map(this::appeal)
        .toList();
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<OfficeTermReviewResult> officeTermReviews(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return officeTermReviews.findProjectionsByPolityIdOrderByDecidedAtDesc(polityId).stream()
        .map(projection -> officeTermReview(polityId, projection))
        .toList();
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<ConstitutionalReviewResult> constitutionalReviews(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return constitutionalReviews.findProjectionsByPolityIdOrderByDecidedAtDesc(polityId).stream()
        .map(projection -> constitutionalReview(polityId, projection))
        .toList();
  }

  private AppealResult appeal(AppealProjection projection) {
    return mapper.toResult(
        projection, memberships.displayName(projection.getAppellantMembershipId()));
  }

  private SanctionResult sanction(SanctionProjection projection, OffsetDateTime now) {
    SanctionStatus status =
        projection.getStatus() == SanctionStatus.ACTIVE && !projection.getEndsAt().isAfter(now)
            ? SanctionStatus.EXPIRED
            : projection.getStatus();
    return mapper.toResult(
        projection, memberships.displayName(projection.getTargetMembershipId()), status);
  }

  private OfficeTermReviewResult officeTermReview(
      UUID polityId, OfficeTermReviewProjection projection) {
    OfficeTermProjection term =
        officeTerms
            .findProjectedByIdAndPolityId(projection.getOfficeTermId(), polityId)
            .orElseThrow(
                () -> ApiException.notFound("office_term_not_found", "Office term not found."));
    OfficeProjection office =
        offices
            .findProjectedById(term.getOfficeId())
            .orElseThrow(() -> ApiException.notFound("office_not_found", "Office not found."));
    return mapper.toResult(
        projection,
        memberships.displayName(projection.getPetitionerMembershipId()),
        term.getMembershipId(),
        memberships.displayName(term.getMembershipId()),
        office.getName(),
        office.getNameKey());
  }

  private ConstitutionalReviewResult constitutionalReview(
      UUID polityId, ConstitutionalReviewProjection projection) {
    OfficialRecordProjection target =
        officialRecords
            .findProjectedByIdAndPolityId(projection.getTargetRecordId(), polityId)
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "official_record_entry_not_found", "Official record entry not found."));
    return mapper.toResult(
        projection,
        target.getEntryNumber(),
        target.getType(),
        memberships.displayName(projection.getPetitionerMembershipId()));
  }
}
