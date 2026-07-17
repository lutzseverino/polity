package com.odonta.polity.workflow;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.resolver.GovernmentAssessmentResolver;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PolityBootstrapCompleter {
  private final GovernmentAssessmentResolver governmentAssessments;
  private final OfficeTermRepository officeTerms;
  private final PolityRepository polities;

  void completeIfReady(UUID polityId, OffsetDateTime now) {
    Polity polity = polities.findEntityById(polityId).orElseThrow(PolityResource.POLITY::notFound);
    if (polity.isBootstrapComplete() || !governmentAssessments.hasFullGovernmentSize(polityId)) {
      return;
    }
    polity.completeBootstrap(now);
    polities.saveAndFlush(polity);
    List<OfficeTerm> bootstrapTerms =
        officeTerms.findEntitiesByPolityIdAndOfficeCodeAndStatusAndAssignedByMotionIdIsNull(
            polityId, Office.STEWARD, OfficeTermStatus.ACTIVE);
    bootstrapTerms.forEach(term -> term.end(now));
    if (!bootstrapTerms.isEmpty()) {
      officeTerms.saveAllAndFlush(bootstrapTerms);
    }
  }
}
