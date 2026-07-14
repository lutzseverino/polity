package com.odonta.polity.mapper;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.result.ConstitutionResult;
import com.odonta.polity.result.ConstitutionalPowerResult;
import com.odonta.polity.result.InstitutionResult;
import com.odonta.polity.result.OfficeResult;
import com.odonta.polity.result.ProcedureResult;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class)
public interface ConstitutionApplicationMapper {
  @BeanMapping(ignoreUnmappedSourceProperties = {"createdAt", "updatedAt", "polityId"})
  @Mapping(target = "id", source = "constitution.id")
  @Mapping(target = "version", source = "constitution.version")
  @Mapping(target = "title", source = "constitution.title")
  @Mapping(target = "body", source = "constitution.body")
  @Mapping(target = "titleKey", source = "constitution.titleKey")
  @Mapping(target = "bodyKey", source = "constitution.bodyKey")
  @Mapping(target = "templateParams", source = "constitution.templateParams")
  @Mapping(target = "status", source = "constitution.status")
  @Mapping(target = "ratifiedAt", source = "constitution.ratifiedAt")
  ConstitutionResult toResult(
      ConstitutionVersion constitution,
      List<InstitutionResult> institutions,
      List<ProcedureResult> procedures,
      List<OfficeResult> offices,
      List<ConstitutionalPowerResult> powers);
}
