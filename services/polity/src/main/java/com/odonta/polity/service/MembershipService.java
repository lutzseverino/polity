package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.mapper.MembershipApplicationMapper;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipResult;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.SanctionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MembershipService {
  private final MembershipApplicationMapper mapper;
  private final MembershipRepository memberships;
  private final SanctionRepository sanctions;

  @PreAuthorize(PolityPermissions.HAS_POLITY_READ)
  public List<MembershipResult> list(UUID polityId, UUID userId) {
    requireActive(polityId, userId);
    return mapper.toResults(
        memberships.findProjectionsByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE));
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

  Membership active(UUID polityId, UUID userId) {
    return memberships
        .findEntityByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
        .orElseThrow(
            () ->
                ApiException.forbidden(
                    "polity_membership_required", "Active membership is required."));
  }

  Membership get(UUID membershipId) {
    return memberships
        .findEntityById(membershipId)
        .orElseThrow(() -> ApiException.notFound("member_not_found", "Member not found."));
  }

  public String displayName(UUID membershipId) {
    return get(membershipId).getDisplayName();
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
