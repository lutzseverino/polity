package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficialRecordType;
import org.springframework.stereotype.Component;

@Component
public class OfficialRecordResponseConversions {

  public com.odonta.polity.api.model.OfficialRecordType toTransport(OfficialRecordType type) {
    return type == null
        ? null
        : com.odonta.polity.api.model.OfficialRecordType.fromValue(type.wireValue());
  }
}
