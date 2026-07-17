package com.odonta.polity.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.polity.api.model.CreateOfficeElectionMotionRequest;
import com.odonta.polity.api.model.RespondOfficeElectionCandidacyRequest;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OfficeElectionTransportMapperTest {
  private final OfficeElectionTransportMapper mapper =
      Mappers.getMapper(OfficeElectionTransportMapper.class);

  @Test
  void mapsElectionIntroductionAtTheOfficeElectionBoundary() {
    UUID officeId = UUID.randomUUID();
    UUID firstCandidateId = UUID.randomUUID();
    UUID secondCandidateId = UUID.randomUUID();
    var request =
        new CreateOfficeElectionMotionRequest(
            officeId, new LinkedHashSet<>(java.util.List.of(firstCandidateId, secondCandidateId)));

    var input = mapper.toInput(request);

    assertThat(input.officeId()).isEqualTo(officeId);
    assertThat(input.candidateMembershipIds()).containsExactly(firstCandidateId, secondCandidateId);
  }

  @Test
  void mapsCandidacyResponseAtTheOfficeElectionBoundary() {
    var input = mapper.toInput(new RespondOfficeElectionCandidacyRequest(true));

    assertThat(input.accepted()).isTrue();
  }
}
