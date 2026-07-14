package com.odonta.polity.mapper;

import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalProjection;
import com.odonta.polity.result.ConstitutionInstitutionChangeResult;
import com.odonta.polity.result.ConstitutionOfficeChangeResult;
import com.odonta.polity.result.ConstitutionPowerChangeResult;
import com.odonta.polity.result.ConstitutionProcedureChangeResult;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class)
public interface ConstitutionAmendmentApplicationMapper {
  @BeanMapping(ignoreUnmappedSourceProperties = "amendmentProposalId")
  ConstitutionInstitutionChangeResult toResult(
      ConstitutionInstitutionChangeProposalProjection change);

  List<ConstitutionInstitutionChangeResult> toInstitutionChangeResults(
      List<ConstitutionInstitutionChangeProposalProjection> changes);

  @BeanMapping(ignoreUnmappedSourceProperties = "amendmentProposalId")
  ConstitutionProcedureChangeResult toResult(ConstitutionProcedureChangeProposalProjection change);

  List<ConstitutionProcedureChangeResult> toProcedureChangeResults(
      List<ConstitutionProcedureChangeProposalProjection> changes);

  @BeanMapping(ignoreUnmappedSourceProperties = "amendmentProposalId")
  @Mapping(target = "code", source = "officeCode")
  ConstitutionOfficeChangeResult toResult(ConstitutionOfficeChangeProposalProjection change);

  List<ConstitutionOfficeChangeResult> toOfficeChangeResults(
      List<ConstitutionOfficeChangeProposalProjection> changes);

  @BeanMapping(ignoreUnmappedSourceProperties = "amendmentProposalId")
  ConstitutionPowerChangeResult toResult(ConstitutionPowerChangeProposalProjection change);

  List<ConstitutionPowerChangeResult> toPowerChangeResults(
      List<ConstitutionPowerChangeProposalProjection> changes);
}
