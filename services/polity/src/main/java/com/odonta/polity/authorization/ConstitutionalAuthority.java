package com.odonta.polity.authorization;

import com.odonta.common.api.ApiException;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.service.MembershipService;
import java.time.Clock;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConstitutionalAuthority {
  private final Clock clock;
  private final MembershipService membershipService;
  private final OfficeTermRepository officeTerms;
  private final ConstitutionalPowerRepository powers;

  public void require(Membership member, ConstitutionVersion constitution, PowerCode code) {
    require(member, constitution, code, true);
  }

  public boolean allows(Membership member, ConstitutionVersion constitution, PowerCode code) {
    return allows(member, constitution, code, true);
  }

  public void requireOwnAppealIntroduction(Membership member, ConstitutionVersion constitution) {
    require(member, constitution, PowerCode.INTRODUCE_APPEAL, false);
  }

  public boolean allowsOwnAppealIntroduction(Membership member, ConstitutionVersion constitution) {
    return allows(member, constitution, PowerCode.INTRODUCE_APPEAL, false);
  }

  public void requireAppealCertification(Membership member, ConstitutionVersion constitution) {
    require(member, constitution, PowerCode.REQUEST_CERTIFICATION, false);
  }

  public boolean allowsAppealCertification(Membership member, ConstitutionVersion constitution) {
    return allows(member, constitution, PowerCode.REQUEST_CERTIFICATION, false);
  }

  private void require(
      Membership member,
      ConstitutionVersion constitution,
      PowerCode code,
      boolean requireStanding) {
    if (!allows(member, constitution, code, requireStanding)) {
      throw ApiException.forbidden(
          "constitutional_authority_missing", "The member lacks constitutional authority.");
    }
  }

  private boolean allows(
      Membership member,
      ConstitutionVersion constitution,
      PowerCode code,
      boolean requireStanding) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    var power =
        powers
            .findEntityByConstitutionVersionIdAndCode(constitution.getId(), code)
            .orElseThrow(
                () ->
                    ApiException.forbidden(
                        "constitutional_power_missing",
                        "The governing constitution does not authorize this action."));
    if (requireStanding) {
      membershipService.requirePoliticalStanding(member.getId(), now);
    }
    if (power.getHolderScope() == PowerHolderScope.ACTIVE_MEMBER) {
      return true;
    }
    return power.getHolderScope() == PowerHolderScope.OFFICE
        && officeTerms.existsByPolityIdAndOfficeCodeAndMembershipIdAndStatusAndEndsAtAfter(
            member.getPolityId(),
            power.getHolderOfficeCode(),
            member.getId(),
            OfficeTermStatus.ACTIVE,
            now);
  }
}
