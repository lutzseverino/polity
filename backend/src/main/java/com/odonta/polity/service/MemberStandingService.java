package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.repository.SanctionRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberStandingService {
  private final SanctionRepository sanctions;

  public boolean hasPoliticalStanding(Membership member, OffsetDateTime at) {
    return member.getStatus() == MembershipStatus.ACTIVE
        && !sanctions.existsByPolityIdAndTargetMembershipIdAndTypeAndStatusAndEndsAtAfter(
            member.getPolityId(),
            member.getId(),
            SanctionType.SUSPENSION,
            SanctionStatus.ACTIVE,
            at);
  }

  public void requirePoliticalStanding(Membership member, OffsetDateTime at) {
    if (!hasPoliticalStanding(member, at)) {
      throw ApiException.forbidden(
          "political_standing_required",
          "This member lacks political standing for this constitutional action.");
    }
  }
}
