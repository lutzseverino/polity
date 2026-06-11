package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConstitutionalAuthority {
  private final ConstitutionalPowerRepository powers;

  public void require(Membership member, ConstitutionVersion constitution, PowerCode code) {
    var power =
        powers
            .findByConstitutionVersionIdAndCode(constitution.getId(), code)
            .orElseThrow(
                () ->
                    ApiException.forbidden(
                        "constitutional_power_missing",
                        "The governing constitution does not authorize this action."));
    if (power.getHolderScope() != PowerHolderScope.ACTIVE_MEMBER
        || member.getStatus() != MembershipStatus.ACTIVE) {
      throw ApiException.forbidden(
          "constitutional_authority_missing", "The member lacks constitutional authority.");
    }
  }
}
