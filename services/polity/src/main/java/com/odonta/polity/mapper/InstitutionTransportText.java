package com.odonta.polity.mapper;

import com.odonta.polity.result.InstitutionResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InstitutionTransportText {
  private final TransportTextResolver text;

  @Named("institutionName")
  public String name(InstitutionResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }
}
