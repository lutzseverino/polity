package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficeResult;
import com.odonta.polity.model.OfficeTermResult;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeTermProjection;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface OfficeApplicationMapper {
  OfficeResult toResult(OfficeProjection projection);

  List<OfficeResult> toResults(List<OfficeProjection> projections);

  OfficeTermResult toResult(OfficeTermProjection projection);

  List<OfficeTermResult> toTermResults(List<OfficeTermProjection> projections);
}
