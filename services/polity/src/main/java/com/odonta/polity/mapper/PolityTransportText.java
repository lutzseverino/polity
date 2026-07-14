package com.odonta.polity.mapper;

import com.odonta.polity.result.PolitySummaryResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolityTransportText {
  private final TransportTextResolver text;

  @Named("summaryInstitutionName")
  public String institutionName(PolitySummaryResult result) {
    return text.resolveName(result.institutionNameKey(), result.institutionName());
  }
}
