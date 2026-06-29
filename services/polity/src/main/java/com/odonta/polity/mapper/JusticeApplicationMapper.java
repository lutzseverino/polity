package com.odonta.polity.mapper;

import com.odonta.polity.model.AppealResult;
import com.odonta.polity.model.ConstitutionalReviewResult;
import com.odonta.polity.model.OfficeTermReviewResult;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.SanctionResult;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.repository.AppealProjection;
import com.odonta.polity.repository.ConstitutionalReviewProjection;
import com.odonta.polity.repository.OfficeTermReviewProjection;
import com.odonta.polity.repository.SanctionProjection;
import java.util.UUID;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class)
public interface JusticeApplicationMapper {

  AppealResult toResult(AppealProjection projection, String appellantName);

  @BeanMapping(ignoreUnmappedSourceProperties = {"motionId", "status"})
  @Mapping(target = "status", source = "resolvedStatus")
  SanctionResult toResult(
      SanctionProjection projection, String targetName, SanctionStatus resolvedStatus);

  OfficeTermReviewResult toResult(
      OfficeTermReviewProjection projection,
      String petitionerName,
      UUID vacatedMembershipId,
      String vacatedMemberName,
      String officeName,
      String officeNameKey);

  ConstitutionalReviewResult toResult(
      ConstitutionalReviewProjection projection,
      int targetEntryNumber,
      OfficialRecordType targetType,
      String petitionerName);
}
