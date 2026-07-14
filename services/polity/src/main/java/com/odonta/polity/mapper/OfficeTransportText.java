package com.odonta.polity.mapper;

import com.odonta.polity.result.OfficeResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficeTransportText {
  private final TransportTextResolver text;

  @Named("officeName")
  public String officeName(OfficeResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }

  @Named("officeDescription")
  public String officeDescription(OfficeResult result) {
    return text.resolveName(result.descriptionKey(), result.description());
  }
}
