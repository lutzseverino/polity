package com.odonta.polity.mapper;

import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.result.OfficeResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface OfficeApplicationMapper {
  OfficeResult toResult(OfficeProjection projection);

  List<OfficeResult> toResults(List<OfficeProjection> projections);
}
