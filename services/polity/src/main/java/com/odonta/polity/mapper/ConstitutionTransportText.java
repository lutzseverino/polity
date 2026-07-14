package com.odonta.polity.mapper;

import com.odonta.polity.result.ConstitutionResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConstitutionTransportText {
  private final TransportTextResolver text;

  @Named("constitutionTitle")
  public String title(ConstitutionResult result) {
    return text.resolve(result.titleKey(), result.title(), result.templateParams());
  }

  @Named("constitutionBody")
  public String body(ConstitutionResult result) {
    return text.resolve(result.bodyKey(), result.body(), result.templateParams());
  }
}
