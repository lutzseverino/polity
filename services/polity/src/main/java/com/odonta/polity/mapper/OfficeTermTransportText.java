package com.odonta.polity.mapper;

import com.odonta.polity.result.OfficeTermResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficeTermTransportText {
  private final TransportTextResolver text;

  @Named("officeTermOfficeName")
  public String officeName(OfficeTermResult result) {
    return text.resolveName(result.officeNameKey(), result.officeName());
  }
}
