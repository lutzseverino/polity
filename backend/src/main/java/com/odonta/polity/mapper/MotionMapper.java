package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CertificationResponse;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.api.model.MotionsResponse;
import com.odonta.polity.api.model.VoteTallyResponse;
import com.odonta.polity.model.MotionDetails;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MotionMapper {
  public MotionResponse toResponse(MotionDetails details) {
    var tally = details.tally();
    MotionResponse response =
        new MotionResponse(
            details.motion().getId(),
            details.motion().getTitle(),
            details.motion().getBody(),
            MotionResponse.StatusEnum.fromValue(details.motion().getStatus().name().toLowerCase()),
            MotionResponse.EffectTypeEnum.ADOPT_RESOLUTION,
            details.constitution().getVersion(),
            details.procedure().getName(),
            details.introducer().getDisplayName(),
            details.motion().getOpenedAt(),
            new VoteTallyResponse(
                tally.yes(),
                tally.no(),
                tally.abstain(),
                tally.eligible(),
                tally.quorumRequired(),
                tally.quorumMet()));
    if (details.certification() != null) {
      response.setCertification(
          new CertificationResponse(
              details.certification().isPassed(),
              details.certification().getExplanation(),
              details.certification().getCertifiedAt()));
    }
    return response;
  }

  public MotionsResponse toResponse(List<MotionDetails> motions) {
    return new MotionsResponse(motions.stream().map(this::toResponse).toList());
  }
}
