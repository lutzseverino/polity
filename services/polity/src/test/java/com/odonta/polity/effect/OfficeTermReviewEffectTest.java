package com.odonta.polity.effect;

import static com.odonta.polity.effect.EffectTestFixtures.NOW;
import static com.odonta.polity.effect.EffectTestFixtures.constitution;
import static com.odonta.polity.effect.EffectTestFixtures.member;
import static com.odonta.polity.effect.EffectTestFixtures.motion;
import static com.odonta.polity.effect.EffectTestFixtures.projection;
import static com.odonta.polity.effect.EffectTestFixtures.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermReview;
import com.odonta.polity.model.OfficeTermReviewProposal;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProposalProjection;
import com.odonta.polity.repository.OfficeTermReviewProposalRepository;
import com.odonta.polity.repository.OfficeTermReviewRepository;
import com.odonta.polity.service.OfficialRecordService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficeTermReviewEffectTest {
  private final OfficeTermReviewProposalRepository proposals =
      mock(OfficeTermReviewProposalRepository.class);
  private final OfficeTermReviewRepository reviews = mock(OfficeTermReviewRepository.class);
  private final OfficeTermRepository terms = mock(OfficeTermRepository.class);
  private final OfficeRepository offices = mock(OfficeRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final OfficeTermReviewEffect effect =
      new OfficeTermReviewEffect(proposals, reviews, terms, offices, memberships, officialRecords);

  @Test
  void vacatesTheActiveOfficeTerm() {
    UUID polityId = UUID.randomUUID();
    var actor = member(polityId, "Actor");
    var holder = member(polityId, "Holder");
    var constitution = constitution(polityId);
    var motion = motion(polityId, actor, constitution, EffectType.VACATE_OFFICE_TERM);
    Office office =
        withId(
            new Office(
                polityId,
                constitution.getId(),
                motion.getJurisdictionId(),
                "steward",
                "Steward",
                "Coordinates",
                14));
    OfficeTerm term =
        withId(
            new OfficeTerm(
                polityId,
                office.getId(),
                office.getCode(),
                holder.getId(),
                null,
                NOW.minusDays(1),
                NOW.plusDays(10)));
    var proposal =
        new OfficeTermReviewProposal(
            polityId, motion.getId(), term.getId(), actor.getId(), "Reason");
    when(proposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(Optional.of(projection(OfficeTermReviewProposalProjection.class, proposal)));
    when(terms.findEntityByIdAndPolityId(term.getId(), polityId)).thenReturn(Optional.of(term));
    when(reviews.existsByPolityIdAndOfficeTermId(polityId, term.getId())).thenReturn(false);
    when(reviews.saveAndFlush(any(OfficeTermReview.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(offices.findEntityByIdAndPolityId(office.getId(), polityId))
        .thenReturn(Optional.of(office));
    when(memberships.findEntityById(holder.getId())).thenReturn(Optional.of(holder));

    effect.apply(motion, actor, constitution, NOW);

    assertThat(term.getStatus()).isEqualTo(OfficeTermStatus.ENDED);
    verify(terms).saveAndFlush(term);
    verify(officialRecords).append(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }
}
