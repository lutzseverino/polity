package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.repository.ConstitutionalReviewProjection;
import com.odonta.polity.result.ConstitutionalReviewResult;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface ConstitutionalReviewApplicationMapper {

  ConstitutionalReviewResult toResult(
      ConstitutionalReviewProjection projection,
      int targetEntryNumber,
      OfficialRecordType targetType,
      String petitionerName);
}
