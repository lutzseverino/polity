package com.odonta.polity.mapper;

import com.odonta.polity.model.MotionResult;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.VoteChoice;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MotionTransportConversions {
  private final TransportTextResolver text;

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
      case INTRODUCE_OFFICE_TERM_REVIEW -> PowerCode.INTRODUCE_OFFICE_TERM_REVIEW;
      case INTRODUCE_CONSTITUTIONAL_REVIEW -> PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW;
      case INTRODUCE_AMENDMENT -> PowerCode.INTRODUCE_AMENDMENT;
      case INTRODUCE_DISBANDMENT -> PowerCode.INTRODUCE_DISBANDMENT;
      case REQUEST_CERTIFICATION -> PowerCode.REQUEST_CERTIFICATION;
    };
  }

  @Named("motionTitle")
  public String title(MotionResult result) {
    return text.resolve(result.titleKey(), result.title(), result.templateParams());
  }

  @Named("motionBody")
  public String body(MotionResult result) {
    return text.resolve(result.bodyKey(), result.body(), result.templateParams());
  }

  @Named("motionProcedureName")
  public String procedureName(MotionResult result) {
    return text.resolveName(result.procedureNameKey(), result.procedureName());
  }
}
