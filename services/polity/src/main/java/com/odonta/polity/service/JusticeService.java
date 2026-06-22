package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.model.AppealResult;
import com.odonta.polity.model.SanctionResult;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.repository.AppealProjection;
import com.odonta.polity.repository.AppealRepository;
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

  private AppealResult appeal(AppealProjection projection) {
    return new AppealResult(
        projection.getId(),
        projection.getSanctionId(),
        projection.getAppellantMembershipId(),
        memberships.displayName(projection.getAppellantMembershipId()),
        projection.getStatus(),
        projection.getReason(),
        projection.getDecidedAt());
  }

  private SanctionResult sanction(SanctionProjection projection, OffsetDateTime now) {
    SanctionStatus status =
        projection.getStatus() == SanctionStatus.ACTIVE && !projection.getEndsAt().isAfter(now)
            ? SanctionStatus.EXPIRED
            : projection.getStatus();
    return new SanctionResult(
        projection.getId(),
        projection.getTargetMembershipId(),
        memberships.displayName(projection.getTargetMembershipId()),
        projection.getType(),
        status,
        projection.getReason(),
        projection.getStartedAt(),
        projection.getEndsAt());
  }
}
