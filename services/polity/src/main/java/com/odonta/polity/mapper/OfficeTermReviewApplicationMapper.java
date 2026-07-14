package com.odonta.polity.mapper;

import com.odonta.polity.repository.OfficeTermReviewProjection;
import com.odonta.polity.result.OfficeTermReviewResult;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface OfficeTermReviewApplicationMapper {

  OfficeTermReviewResult toResult(
      OfficeTermReviewProjection projection,
      String petitionerName,
      UUID vacatedMembershipId,
      String vacatedMemberName,
      String officeName,
      String officeNameKey);
}
