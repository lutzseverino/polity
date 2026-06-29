package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficeResult;
import com.odonta.polity.model.OfficeTermResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficeTransportConversions {
  private final TransportTextResolver text;

  @Named("officeName")
  public String officeName(OfficeResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }

  @Named("officeDescription")
  public String officeDescription(OfficeResult result) {
    return text.resolveName(result.descriptionKey(), result.description());
  }

  @Named("officeTermOfficeName")
  public String officeTermOfficeName(OfficeTermResult result) {
    return text.resolveName(result.officeNameKey(), result.officeName());
  }
}
