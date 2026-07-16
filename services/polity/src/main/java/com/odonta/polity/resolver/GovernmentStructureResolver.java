package com.odonta.polity.resolver;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.mapper.GovernmentFormationApplicationMapper;
import com.odonta.polity.mapper.GovernmentStructureApplicationMapper;
import com.odonta.polity.mapper.JurisdictionApplicationMapper;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.result.GovernmentStructureResult;
import com.odonta.polity.service.ConstitutionService;
import java.util.Comparator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GovernmentStructureResolver {
  private final ConstitutionService constitutions;
  private final JurisdictionApplicationMapper jurisdictionMapper;
  private final JurisdictionRepository jurisdictions;
  private final GovernmentFormationApplicationMapper formationMapper;
  private final GovernmentAssessmentResolver assessments;
  private final PolityRepository polities;
  private final GovernmentStructureApplicationMapper mapper;

  @Transactional(readOnly = true)
  public GovernmentStructureResult resolve(UUID polityId, UUID userId) {
    var constitution = constitutions.get(polityId, userId);
    var polity = polities.findEntityById(polityId).orElseThrow(PolityResource.POLITY::notFound);
    var jurisdictionResults =
        jurisdictions.findProjectionsByPolityId(polityId).stream()
            .map(jurisdictionMapper::toResult)
            .sorted(Comparator.comparing(result -> result.kind().name()))
            .toList();
    var formation =
        formationMapper.toResult(
            polity.isBootstrapComplete(),
            polity.getBootstrapCompletedAt(),
            assessments.minimumFullGovernmentMembers(),
            assessments.activeMemberCount(polityId),
            assessments.standingMemberCount(polityId));
    return mapper.toResult(constitution, jurisdictionResults, formation);
  }
}
