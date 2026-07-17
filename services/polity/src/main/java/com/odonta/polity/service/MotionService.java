package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.resolver.MotionResultResolver;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.result.PageResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MotionService {
  private final PolityAccessPolicy access;
  private final MembershipRepository memberships;
  private final MotionRepository motions;
  private final MotionResultResolver results;

  @Transactional(readOnly = true)
  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PageResult<MotionResult> list(UUID polityId, UUID userId, int page, int size) {
    access.requireReadable(polityId, userId);
    UUID currentMembershipId = currentMembershipId(polityId, userId);
    Page<MotionProjection> projections =
        motions.findProjectionsByPolityIdOrderByOpenedAtDescIdAsc(
            polityId, PageRequest.of(page, size));
    return new PageResult<>(
        results.resolveAll(polityId, projections.getContent(), currentMembershipId),
        projections.getNumber(),
        projections.getSize(),
        projections.getTotalElements());
  }

  @Transactional(readOnly = true)
  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public MotionResult get(UUID polityId, UUID motionId, UUID userId) {
    access.requireReadable(polityId, userId);
    return results.resolve(polityId, motionId, currentMembershipId(polityId, userId));
  }

  private UUID currentMembershipId(UUID polityId, UUID userId) {
    return memberships
        .findProjectedByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
        .map(MembershipProjection::getId)
        .orElse(null);
  }
}
