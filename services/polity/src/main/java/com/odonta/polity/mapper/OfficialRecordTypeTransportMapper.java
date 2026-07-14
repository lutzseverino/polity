package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficialRecordType;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface OfficialRecordTypeTransportMapper {
  default com.odonta.polity.api.model.OfficialRecordType toTransport(OfficialRecordType type) {
    return type == null
        ? null
        : com.odonta.polity.api.model.OfficialRecordType.fromValue(type.wireValue());
  }
}
