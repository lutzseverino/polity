package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.JusticeApplicationMapper;
import com.odonta.polity.model.AppealResult;
import com.odonta.polity.model.SanctionResult;
import com.odonta.polity.model.SanctionStatus;
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
  private final JusticeApplicationMapper mapper;
  private final SanctionRepository sanctions;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<SanctionResult> sanctions(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    OffsetDateTime now = OffsetDateTime.now(clock);
    return sanctions.findProjectionsByPolityId(polityId).stream()
        .map(projection -> toResult(projection, now))
        .toList();
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<AppealResult> appeals(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return mapper.toAppealResults(appeals.findProjectionsByPolityId(polityId));
  }

  private SanctionStatus statusAt(SanctionProjection projection, OffsetDateTime now) {
    if (projection.getStatus() == SanctionStatus.ACTIVE && !projection.getEndsAt().isAfter(now)) {
      return SanctionStatus.EXPIRED;
    }
    return projection.getStatus();
  }

  private SanctionResult toResult(SanctionProjection projection, OffsetDateTime now) {
    return new SanctionResult(
        projection.getId(),
        projection.getTargetMembershipId(),
        projection.getTargetName(),
        projection.getType(),
        statusAt(projection, now),
        projection.getReason(),
        projection.getStartedAt(),
        projection.getEndsAt());
  }
}
