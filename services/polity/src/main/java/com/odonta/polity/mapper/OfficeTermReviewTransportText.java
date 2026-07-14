package com.odonta.polity.mapper;

import com.odonta.polity.result.OfficeTermReviewResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficeTermReviewTransportText {
  private final TransportTextResolver text;

  @Named("officeTermReviewOfficeName")
  public String officeTermReviewOfficeName(OfficeTermReviewResult result) {
    return text.resolveName(result.officeNameKey(), result.officeName());
  }
}
