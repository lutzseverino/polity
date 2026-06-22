package com.odonta.polity.mapper;

import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.VoteChoice;
import org.springframework.stereotype.Component;

@Component
public class MotionTransportConversions {

  public VoteChoice toDomain(com.odonta.polity.api.model.VoteChoice choice) {
    if (choice == null) {
      return null;
    }
    return switch (choice) {
      case YES -> VoteChoice.YES;
      case NO -> VoteChoice.NO;
      case ABSTAIN -> VoteChoice.ABSTAIN;
    };
  }

  public PowerCode toDomain(com.odonta.polity.api.model.AmendablePowerCode powerCode) {
    if (powerCode == null) {
      return null;
    }
    return switch (powerCode) {
      case ADMIT_MEMBER -> PowerCode.ADMIT_MEMBER;
      case INTRODUCE_MOTION -> PowerCode.INTRODUCE_MOTION;
      case INTRODUCE_OFFICE_ELECTION -> PowerCode.INTRODUCE_OFFICE_ELECTION;
      case INTRODUCE_SANCTION -> PowerCode.INTRODUCE_SANCTION;
      case INTRODUCE_APPEAL -> PowerCode.INTRODUCE_APPEAL;
      case INTRODUCE_AMENDMENT -> PowerCode.INTRODUCE_AMENDMENT;
      case INTRODUCE_DISBANDMENT -> PowerCode.INTRODUCE_DISBANDMENT;
      case REQUEST_CERTIFICATION -> PowerCode.REQUEST_CERTIFICATION;
    };
  }
}
