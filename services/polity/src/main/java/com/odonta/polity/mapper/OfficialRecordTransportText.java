package com.odonta.polity.mapper;

import com.odonta.polity.result.OfficialRecordResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficialRecordTransportText {
  private final TransportTextResolver text;

  @Named("officialRecordTitle")
  public String title(OfficialRecordResult result) {
    return text.resolve(result.titleKey(), result.title(), result.templateParams());
  }

  @Named("officialRecordBody")
  public String body(OfficialRecordResult result) {
    return text.resolve(result.bodyKey(), result.body(), result.templateParams());
  }
}
