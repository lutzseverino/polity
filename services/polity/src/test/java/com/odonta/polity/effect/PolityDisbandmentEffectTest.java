package com.odonta.polity.effect;

import static com.odonta.polity.effect.EffectTestFixtures.NOW;
import static com.odonta.polity.effect.EffectTestFixtures.constitution;
import static com.odonta.polity.effect.EffectTestFixtures.member;
import static com.odonta.polity.effect.EffectTestFixtures.motion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.PolityResources;
import com.odonta.polity.authorization.PolityRevocationPlanner;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.service.OfficialRecordService;
import io.github.lutzseverino.cardo.authorization.grant.RevocationPlan;
import io.github.lutzseverino.cardo.authorization.grant.Revocations;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolityDisbandmentEffectTest {
  private final PolityRepository polities = mock(PolityRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final PolityRevocationPlanner revocationPlanner = mock(PolityRevocationPlanner.class);
  private final Revocations revocations = mock(Revocations.class);
  private final PolityDisbandmentEffect effect =
      new PolityDisbandmentEffect(
          polities, officeTerms, memberships, officialRecords, revocationPlanner, revocations);

  @Test
  void disbandsThePolityAndEndsActiveTerms() {
    UUID polityId = UUID.randomUUID();
    var actor = member(polityId, "Actor");
    var constitution = constitution(polityId);
    var motion = motion(polityId, actor, constitution, EffectType.DISBAND_POLITY);
    Polity polity = new Polity("Commons", PolityVisibility.PRIVATE, UUID.randomUUID());
    org.springframework.test.util.ReflectionTestUtils.setField(polity, "id", polityId);
    OfficeTerm term =
        new OfficeTerm(
            polityId,
            UUID.randomUUID(),
            "steward",
            actor.getId(),
            NOW.minusDays(1),
            NOW.plusDays(10));
    RevocationPlan plan =
        RevocationPlan.builder()
            .revokeActions(
                actor.getAuthorizationSubject(),
                PolityResources.POLITY.resource(polityId),
                List.of(PolityPermissions.PARTICIPATE))
            .build();
    when(polities.findEntityById(polityId)).thenReturn(Optional.of(polity));
    when(officeTerms.findEntitiesByPolityIdAndStatus(polityId, OfficeTermStatus.ACTIVE))
        .thenReturn(List.of(term));
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor));
    when(revocationPlanner.participation(actor.getAuthorizationSubject(), polityId))
        .thenReturn(plan);

    effect.apply(motion, actor, constitution, NOW);

    assertThat(polity.getStatus()).isEqualTo(PolityStatus.DISBANDED);
    assertThat(term.getStatus()).isEqualTo(OfficeTermStatus.ENDED);
    verify(revocations).stage(plan);
    verify(polities).saveAndFlush(polity);
    verify(officialRecords).append(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }
}
