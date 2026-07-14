package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ConstitutionalReviewResponse;
import com.odonta.polity.result.ConstitutionalReviewResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class, uses = OfficialRecordTypeTransportMapper.class)
public interface ConstitutionalReviewTransportMapper {
  ConstitutionalReviewResponse toResponse(ConstitutionalReviewResult result);

  List<ConstitutionalReviewResponse> toResponses(List<ConstitutionalReviewResult> results);
}
