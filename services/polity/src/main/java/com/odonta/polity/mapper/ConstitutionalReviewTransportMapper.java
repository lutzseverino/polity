package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ConstitutionalReviewResponse;
import com.odonta.polity.api.model.CreateConstitutionalReviewMotionRequest;
import com.odonta.polity.input.CreateConstitutionalReviewMotionInput;
import com.odonta.polity.result.ConstitutionalReviewResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class, uses = OfficialRecordTypeTransportMapper.class)
public interface ConstitutionalReviewTransportMapper {
  CreateConstitutionalReviewMotionInput toInput(CreateConstitutionalReviewMotionRequest request);

  ConstitutionalReviewResponse toResponse(ConstitutionalReviewResult result);

  List<ConstitutionalReviewResponse> toResponses(List<ConstitutionalReviewResult> results);
}
