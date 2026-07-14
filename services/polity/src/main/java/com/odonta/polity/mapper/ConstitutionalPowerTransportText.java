package com.odonta.polity.mapper;

import com.odonta.polity.result.ConstitutionalPowerResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConstitutionalPowerTransportText {
  private final TransportTextResolver text;

  @Named("powerName")
  public String name(ConstitutionalPowerResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }
}
