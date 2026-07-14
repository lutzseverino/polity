package com.odonta.polity.mapper;

import com.odonta.polity.result.ConstitutionResult;
import com.odonta.polity.result.GovernmentFormationResult;
import com.odonta.polity.result.GovernmentStructureResult;
import com.odonta.polity.result.JurisdictionResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface GovernmentStructureApplicationMapper {
  GovernmentStructureResult toResult(
      ConstitutionResult constitution,
      List<JurisdictionResult> jurisdictions,
      GovernmentFormationResult formation);
}
