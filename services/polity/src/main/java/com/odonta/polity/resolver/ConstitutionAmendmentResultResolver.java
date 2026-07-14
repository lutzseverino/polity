package com.odonta.polity.resolver;

import com.odonta.polity.mapper.ConstitutionAmendmentApplicationMapper;
import com.odonta.polity.repository.ConstitutionAmendmentProposalProjection;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.result.ConstitutionAmendmentProposalResult;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConstitutionAmendmentResultResolver {
  private final ConstitutionAmendmentApplicationMapper mapper;
  private final ConstitutionAmendmentProposalRepository proposals;
  private final ConstitutionInstitutionChangeProposalRepository institutionChanges;
  private final ConstitutionProcedureChangeProposalRepository procedureChanges;
  private final ConstitutionOfficeChangeProposalRepository officeChanges;
  private final ConstitutionPowerChangeProposalRepository powerChanges;

  public Map<UUID, ConstitutionAmendmentProposalResult> resolveAll(
      UUID polityId, Collection<UUID> motionIds) {
    if (motionIds.isEmpty()) {
      return Map.of();
    }
    List<ConstitutionAmendmentProposalProjection> proposalList =
        proposals.findProjectionsByPolityIdAndMotionIdIn(polityId, motionIds);
    if (proposalList.isEmpty()) {
      return Map.of();
    }
    List<UUID> proposalIds =
        proposalList.stream()
            .map(ConstitutionAmendmentProposalProjection::getId)
            .distinct()
            .toList();
    var institutions =
        institutionChanges
            .findProjectionsByPolityIdAndAmendmentProposalIdIn(polityId, proposalIds)
            .stream()
            .collect(Collectors.groupingBy(change -> change.getAmendmentProposalId()));
    var procedures =
        procedureChanges
            .findProjectionsByPolityIdAndAmendmentProposalIdIn(polityId, proposalIds)
            .stream()
            .collect(Collectors.groupingBy(change -> change.getAmendmentProposalId()));
    var offices =
        officeChanges
            .findProjectionsByPolityIdAndAmendmentProposalIdIn(polityId, proposalIds)
            .stream()
            .collect(Collectors.groupingBy(change -> change.getAmendmentProposalId()));
    var powers =
        powerChanges
            .findProjectionsByPolityIdAndAmendmentProposalIdIn(polityId, proposalIds)
            .stream()
            .collect(Collectors.groupingBy(change -> change.getAmendmentProposalId()));
    return proposalList.stream()
        .collect(
            Collectors.toMap(
                ConstitutionAmendmentProposalProjection::getMotionId,
                proposal ->
                    new ConstitutionAmendmentProposalResult(
                        proposal.getTitle(),
                        proposal.getBody(),
                        mapper.toInstitutionChangeResults(
                            institutions.getOrDefault(proposal.getId(), List.of())),
                        mapper.toProcedureChangeResults(
                            procedures.getOrDefault(proposal.getId(), List.of())),
                        mapper.toOfficeChangeResults(
                            offices.getOrDefault(proposal.getId(), List.of())),
                        mapper.toPowerChangeResults(
                            powers.getOrDefault(proposal.getId(), List.of()))),
                (first, ignored) -> first));
  }
}
