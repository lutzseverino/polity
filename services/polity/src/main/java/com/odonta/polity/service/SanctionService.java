package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.mapper.SanctionApplicationMapper;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.repository.SanctionProjection;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.result.PageResult;
import com.odonta.polity.result.SanctionResult;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SanctionService {
  private final PolityAccessPolicy access;
  private final Clock clock;
  private final SanctionApplicationMapper mapper;
  private final MembershipService memberships;
  private final SanctionRepository sanctions;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PageResult<SanctionResult> list(UUID polityId, UUID userId, int page, int size) {
    access.requireReadable(polityId, userId);
    OffsetDateTime now = OffsetDateTime.now(clock);
    Page<SanctionProjection> pageResult =
        sanctions.findProjectionsByPolityIdOrderByStartedAtDescIdAsc(
            polityId, PageRequest.of(page, size));
    List<SanctionProjection> projections = pageResult.getContent();
    if (projections.isEmpty()) {
      return new PageResult<>(
          List.of(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }
    Map<UUID, String> memberNames =
        memberships.displayNames(
            polityId, projections.stream().map(SanctionProjection::getTargetMembershipId).toList());
    return new PageResult<>(
        projections.stream().map(projection -> result(projection, memberNames, now)).toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements());
  }

  private SanctionResult result(
      SanctionProjection projection, Map<UUID, String> memberNames, OffsetDateTime now) {
    SanctionStatus status =
        projection.getStatus() == SanctionStatus.ACTIVE && !projection.getEndsAt().isAfter(now)
            ? SanctionStatus.EXPIRED
            : projection.getStatus();
    return mapper.toResult(
        projection, requiredTargetName(memberNames, projection.getTargetMembershipId()), status);
  }

  private String requiredTargetName(Map<UUID, String> memberNames, UUID targetMembershipId) {
    String targetName = memberNames.get(targetMembershipId);
    if (targetName == null) {
      throw PolityResource.MEMBER.notFound();
    }
    return targetName;
  }
}
