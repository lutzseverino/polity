package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.mapper.MembershipApplicationMapper;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.result.MembershipResult;
import com.odonta.polity.result.PageResult;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MembershipService {
  private final MembershipApplicationMapper mapper;
  private final MembershipRepository memberships;
  private final SanctionRepository sanctions;

  @PreAuthorize(PolityPermissions.HAS_POLITY_READ)
  public PageResult<MembershipResult> list(UUID polityId, UUID userId, int page, int size) {
    requireActive(polityId, userId);
    Page<MembershipProjection> projections =
        memberships.findProjectionsByPolityIdAndStatusOrderByAdmittedAtAscIdAsc(
            polityId, MembershipStatus.ACTIVE, PageRequest.of(page, size));
    return new PageResult<>(
        mapper.toResults(projections.getContent()),
        projections.getNumber(),
        projections.getSize(),
        projections.getTotalElements());
  }

  public void requireActive(UUID polityId, UUID userId) {
    active(polityId, userId);
  }

  public void requirePoliticalStanding(UUID membershipId, OffsetDateTime at) {
    requirePoliticalStanding(get(membershipId), at);
  }

  public boolean hasPoliticalStanding(UUID membershipId, OffsetDateTime at) {
    return hasPoliticalStanding(get(membershipId), at);
  }

  public Set<UUID> politicalStanding(
      UUID polityId, Collection<UUID> membershipIds, OffsetDateTime at) {
    if (membershipIds.isEmpty()) {
      return Set.of();
    }
    List<UUID> uniqueMembershipIds = membershipIds.stream().distinct().toList();
    Set<UUID> suspendedMembershipIds =
        sanctions
            .findProjectionsByPolityIdAndTargetMembershipIdInAndTypeAndStatusAndEndsAtAfter(
                polityId, uniqueMembershipIds, SanctionType.SUSPENSION, SanctionStatus.ACTIVE, at)
            .stream()
            .map(sanction -> sanction.getTargetMembershipId())
            .collect(Collectors.toSet());
    return memberships.findProjectionsByPolityIdAndIdIn(polityId, uniqueMembershipIds).stream()
        .filter(member -> member.getStatus() == MembershipStatus.ACTIVE)
        .map(MembershipProjection::getId)
        .filter(id -> !suspendedMembershipIds.contains(id))
        .collect(Collectors.toSet());
  }

  Membership active(UUID polityId, UUID userId) {
    return memberships
        .findEntityByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
        .orElseThrow(
            () ->
                ApiException.forbidden(
                    "polity_membership_required", "Active membership is required."));
  }

  Membership get(UUID membershipId) {
    return memberships.findEntityById(membershipId).orElseThrow(PolityResource.MEMBER::notFound);
  }

  public String displayName(UUID membershipId) {
    return get(membershipId).getDisplayName();
  }

  Map<UUID, String> displayNames(UUID polityId, Collection<UUID> membershipIds) {
    List<UUID> uniqueMembershipIds = membershipIds.stream().distinct().toList();
    return memberships.findProjectionsByPolityIdAndIdIn(polityId, uniqueMembershipIds).stream()
        .collect(
            Collectors.toMap(
                MembershipProjection::getId,
                MembershipProjection::getDisplayName,
                (first, ignored) -> first));
  }

  boolean hasPoliticalStanding(Membership member, OffsetDateTime at) {
    return member.getStatus() == MembershipStatus.ACTIVE
        && !sanctions.existsByPolityIdAndTargetMembershipIdAndTypeAndStatusAndEndsAtAfter(
            member.getPolityId(),
            member.getId(),
            SanctionType.SUSPENSION,
            SanctionStatus.ACTIVE,
            at);
  }

  void requirePoliticalStanding(Membership member, OffsetDateTime at) {
    if (!hasPoliticalStanding(member, at)) {
      throw ApiException.forbidden(
          "political_standing_required",
          "This member lacks political standing for this constitutional action.");
    }
  }
}
