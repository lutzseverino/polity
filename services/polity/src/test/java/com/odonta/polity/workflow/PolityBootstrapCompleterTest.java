package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.resolver.GovernmentAssessmentResolver;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PolityBootstrapCompleterTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  private final GovernmentAssessmentResolver governmentAssessments =
      mock(GovernmentAssessmentResolver.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final PolityBootstrapCompleter completer =
      new PolityBootstrapCompleter(governmentAssessments, officeTerms, polities);

  @Test
  void completeIfReadyEndsInitialStewardTermAtFullGovernmentSize() {
    UUID polityId = UUID.randomUUID();
    Polity polity = polity(polityId);
    OfficeTerm bootstrapTerm =
        new OfficeTerm(
            polityId,
            UUID.randomUUID(),
            Office.STEWARD,
            UUID.randomUUID(),
            NOW.minusDays(1),
            NOW.plusDays(14));
    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(governmentAssessments.hasFullGovernmentSize(polityId)).thenReturn(true);
    when(officeTerms.findEntitiesByPolityIdAndOfficeCodeAndStatusAndAssignedByMotionIdIsNull(
            polityId, Office.STEWARD, OfficeTermStatus.ACTIVE))
        .thenReturn(List.of(bootstrapTerm));

    completer.completeIfReady(polityId, NOW);

    assertThat(bootstrapTerm.getStatus()).isEqualTo(OfficeTermStatus.ENDED);
    assertThat(bootstrapTerm.getEndedAt()).isEqualTo(NOW);
    verify(polities).saveAndFlush(polity);
    verify(officeTerms).saveAllAndFlush(List.of(bootstrapTerm));
  }

  @Test
  void completeIfReadyLeavesStewardTermBeforeFullGovernmentSize() {
    UUID polityId = UUID.randomUUID();
    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity(polityId)));
    when(governmentAssessments.hasFullGovernmentSize(polityId)).thenReturn(false);

    completer.completeIfReady(polityId, NOW);

    verify(officeTerms, never())
        .findEntitiesByPolityIdAndOfficeCodeAndStatusAndAssignedByMotionIdIsNull(
            polityId, Office.STEWARD, OfficeTermStatus.ACTIVE);
    verify(officeTerms, never()).saveAllAndFlush(any());
    verify(polities, never()).saveAndFlush(any());
  }

  private Polity polity(UUID polityId) {
    Polity polity = new Polity("Pocket Republic", PolityVisibility.PRIVATE, UUID.randomUUID());
    ReflectionTestUtils.setField(polity, "id", polityId);
    return polity;
  }
}
