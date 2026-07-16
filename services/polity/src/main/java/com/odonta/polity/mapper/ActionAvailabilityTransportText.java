package com.odonta.polity.mapper;

import com.odonta.polity.result.ActionAvailabilityResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActionAvailabilityTransportText {
  private final TransportTextResolver text;

  @Named("availabilityReasonMessage")
  public String reasonMessage(ActionAvailabilityResult result) {
    if (result == null || result.reason() == null) {
      return null;
    }
    String reason = result.reason().wireValue();
    return text.resolveName("api_error." + reason, reason);
  }
}
