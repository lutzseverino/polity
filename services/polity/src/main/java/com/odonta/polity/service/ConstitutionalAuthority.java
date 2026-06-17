package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConstitutionalAuthority {
  private final Clock clock;
  private final MemberStandingService standing;
  private final OfficeTermRepository officeTerms;
  private final ConstitutionalPowerRepository powers;

  public void require(Membership member, ConstitutionVersion constitution, PowerCode code) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    var power =
        powers
            .findByConstitutionVersionIdAndCode(constitution.getId(), code)
            .orElseThrow(
                () ->
                    ApiException.forbidden(
                        "constitutional_power_missing",
                        "The governing constitution does not authorize this action."));
    if (code != PowerCode.INTRODUCE_APPEAL) {
      standing.requirePoliticalStanding(member, now);
    }
    if (power.getHolderScope() == PowerHolderScope.ACTIVE_MEMBER) {
      return;
    }
    if (power.getHolderScope() == PowerHolderScope.OFFICE
        && officeTerms.existsByPolityIdAndOfficeCodeAndMembershipIdAndStatusAndEndsAtAfter(
            member.getPolityId(),
            power.getHolderOfficeCode(),
            member.getId(),
            OfficeTermStatus.ACTIVE,
            now)) {
      return;
    }
    throw ApiException.forbidden(
        "constitutional_authority_missing", "The member lacks constitutional authority.");
  }
}
