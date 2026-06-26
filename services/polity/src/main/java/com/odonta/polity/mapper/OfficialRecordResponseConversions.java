package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficialRecordResult;
import com.odonta.polity.model.OfficialRecordType;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficialRecordResponseConversions {
  private final TransportTextResolver text;

  public com.odonta.polity.api.model.OfficialRecordType toTransport(OfficialRecordType type) {
    return type == null
        ? null
        : com.odonta.polity.api.model.OfficialRecordType.fromValue(type.wireValue());
  }

  @Named("officialRecordTitle")
  public String title(OfficialRecordResult result) {
    return text.resolve(result.titleKey(), result.title(), result.templateParams());
  }

  @Named("officialRecordBody")
  public String body(OfficialRecordResult result) {
    return text.resolve(result.bodyKey(), result.body(), result.templateParams());
  }
}
