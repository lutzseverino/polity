package com.odonta.polity.mapper;

import com.odonta.polity.model.AppealResult;
import com.odonta.polity.model.SanctionResult;
import com.odonta.polity.repository.AppealProjection;
import com.odonta.polity.repository.SanctionProjection;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface JusticeApplicationMapper {
  SanctionResult toResult(SanctionProjection projection);

  List<SanctionResult> toSanctionResults(List<SanctionProjection> projections);

  AppealResult toResult(AppealProjection projection);

  List<AppealResult> toAppealResults(List<AppealProjection> projections);
}
