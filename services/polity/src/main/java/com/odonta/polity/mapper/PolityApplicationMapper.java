package com.odonta.polity.mapper;

import com.odonta.polity.model.ConstitutionBootstrapResult;
import com.odonta.polity.model.ConstitutionInstitutionResult;
import com.odonta.polity.model.ConstitutionJurisdictionResult;
import com.odonta.polity.model.ConstitutionPowerResult;
import com.odonta.polity.model.ConstitutionProcedureResult;
import com.odonta.polity.model.ConstitutionResult;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.OfficeResult;
import com.odonta.polity.model.PolityResult;
import com.odonta.polity.repository.ConstitutionInstitutionProjection;
import com.odonta.polity.repository.ConstitutionJurisdictionProjection;
import com.odonta.polity.repository.ConstitutionPowerProjection;
import com.odonta.polity.repository.ConstitutionProcedureProjection;
import com.odonta.polity.repository.PolityProjection;
import java.time.OffsetDateTime;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(config = PolityMapperConfig.class)
public interface PolityApplicationMapper {

  PolityResult toResult(
      PolityProjection projection,
      int constitutionVersion,
      String jurisdictionName,
      String institutionName,
      String institutionNameKey);

  @BeanMapping(
      ignoreUnmappedSourceProperties = {
        "createdAt",
        "updatedAt",
        "polityId",
        "title",
        "body",
        "titleKey",
        "bodyKey",
        "templateParams"
      })
  @Mapping(target = "id", source = "constitution.id")
  @Mapping(target = "version", source = "constitution.version")
  @Mapping(target = "status", source = "constitution.status")
  @Mapping(target = "ratifiedAt", source = "constitution.ratifiedAt")
  ConstitutionResult toConstitutionResult(
      ConstitutionVersion constitution,
      List<ConstitutionJurisdictionResult> jurisdictions,
      List<ConstitutionInstitutionResult> institutions,
      List<ConstitutionProcedureResult> procedures,
      List<OfficeResult> offices,
      List<ConstitutionPowerResult> powers,
      ConstitutionBootstrapResult bootstrap);

  @BeanMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
  ConstitutionBootstrapResult toBootstrapResult(
      boolean complete,
      OffsetDateTime completedAt,
      int minimumFullGovernmentMembers,
      long activeMemberCount,
      long standingMemberCount);

  ConstitutionJurisdictionResult toResult(ConstitutionJurisdictionProjection projection);

  ConstitutionInstitutionResult toResult(ConstitutionInstitutionProjection projection);

  ConstitutionProcedureResult toResult(ConstitutionProcedureProjection projection);

  ConstitutionPowerResult toResult(ConstitutionPowerProjection projection);
}
