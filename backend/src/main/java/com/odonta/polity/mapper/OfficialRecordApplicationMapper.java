package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficialRecordResult;
import com.odonta.polity.repository.OfficialRecordProjection;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface OfficialRecordApplicationMapper {

  OfficialRecordResult toResult(OfficialRecordProjection projection);

  List<OfficialRecordResult> toResults(List<OfficialRecordProjection> projections);
}
