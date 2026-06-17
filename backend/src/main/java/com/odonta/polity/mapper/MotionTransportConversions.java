package com.odonta.polity.mapper;

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
}
