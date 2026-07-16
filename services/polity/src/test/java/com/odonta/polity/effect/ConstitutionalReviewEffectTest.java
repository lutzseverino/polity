package com.odonta.polity.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalReview;
import com.odonta.polity.model.ConstitutionalReviewProposal;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Resolution;
import com.odonta.polity.model.ResolutionStatus;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.repository.ConstitutionalReviewProposalProjection;
import com.odonta.polity.repository.ConstitutionalReviewProposalRepository;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.service.OfficialRecordService;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ConstitutionalReviewEffectTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-14T10:00:00Z");

  private final ConstitutionalReviewProposalRepository constitutionalReviewProposals =
      mock(ConstitutionalReviewProposalRepository.class);
  private final ConstitutionalReviewRepository constitutionalReviews =
      mock(ConstitutionalReviewRepository.class);
  private final OfficialRecordRepository officialRecordEntries =
      mock(OfficialRecordRepository.class);
  private final OfficeTermRepository officeTerms = mock(OfficeTermRepository.class);
  private final ResolutionRepository resolutions = mock(ResolutionRepository.class);
  private final SanctionRepository sanctions = mock(SanctionRepository.class);
  private final OfficialRecordService officialRecords = mock(OfficialRecordService.class);
  private final OfficialActVoidRemedy remedy =
      new OfficialActVoidRemedy(officeTerms, resolutions, sanctions);
  private final ConstitutionalReviewEffect effect =
      new ConstitutionalReviewEffect(
          constitutionalReviewProposals,
          constitutionalReviews,
          officialRecordEntries,
          remedy,
          officialRecords);

  @Test
  void constitutionalReviewEffectVoidsOfficialActAndVacatesActiveSanction() {
    UUID polityId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    UUID sanctionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion =
        motion(polityId, actor.getId(), constitution.getId(), EffectType.VOID_OFFICIAL_ACT);
    Sanction sanction =
        new Sanction(
            polityId,
            UUID.randomUUID(),
            actor.getId(),
            SanctionType.WARNING,
            "Reason",
            NOW.minusDays(1),
            NOW.plusDays(3));
    ReflectionTestUtils.setField(sanction, "id", sanctionId);
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);
    ConstitutionalReviewProposal proposal =
        new ConstitutionalReviewProposal(
            polityId, motion.getId(), targetRecordId, actor.getId(), "Wrong authority");

    when(targetRecord.getId()).thenReturn(targetRecordId);
    when(targetRecord.getPolityId()).thenReturn(polityId);
    when(targetRecord.getEntryNumber()).thenReturn(9);
    when(targetRecord.getType()).thenReturn(OfficialRecordType.SANCTION_APPLIED);
    when(targetRecord.getSourceId()).thenReturn(sanctionId);
    when(constitutionalReviewProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionalReviewProposalProjection.class, proposal)));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));
    when(sanctions.findEntityByIdAndPolityId(sanctionId, polityId))
        .thenReturn(Optional.of(sanction));
    when(constitutionalReviews.saveAndFlush(any(ConstitutionalReview.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    effect.apply(motion, actor, constitution, NOW);

    assertThat(sanction.getStatus()).isEqualTo(com.odonta.polity.model.SanctionStatus.VACATED);
    verify(sanctions).saveAndFlush(sanction);
    ArgumentCaptor<ConstitutionalReview> reviewCaptor =
        ArgumentCaptor.forClass(ConstitutionalReview.class);
    verify(constitutionalReviews).saveAndFlush(reviewCaptor.capture());
    assertThat(reviewCaptor.getValue().getTargetRecordId()).isEqualTo(targetRecordId);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.OFFICIAL_ACT_VOIDED),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void constitutionalReviewEffectVoidsAdoptedResolution() {
    UUID polityId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    UUID resolutionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion =
        motion(polityId, actor.getId(), constitution.getId(), EffectType.VOID_OFFICIAL_ACT);
    Resolution resolution =
        new Resolution(
            polityId, UUID.randomUUID(), "Budget resolution", "Buy more gavels.", NOW.minusDays(1));
    ReflectionTestUtils.setField(resolution, "id", resolutionId);
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);
    ConstitutionalReviewProposal proposal =
        new ConstitutionalReviewProposal(
            polityId, motion.getId(), targetRecordId, actor.getId(), "Unconstitutional");

    when(targetRecord.getId()).thenReturn(targetRecordId);
    when(targetRecord.getPolityId()).thenReturn(polityId);
    when(targetRecord.getEntryNumber()).thenReturn(11);
    when(targetRecord.getType()).thenReturn(OfficialRecordType.RESOLUTION_ADOPTED);
    when(targetRecord.getSourceId()).thenReturn(resolutionId);
    when(constitutionalReviewProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionalReviewProposalProjection.class, proposal)));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));
    when(resolutions.findEntityByIdAndPolityId(resolutionId, polityId))
        .thenReturn(Optional.of(resolution));
    when(constitutionalReviews.saveAndFlush(any(ConstitutionalReview.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    effect.apply(motion, actor, constitution, NOW);

    assertThat(resolution.getStatus()).isEqualTo(ResolutionStatus.VOIDED);
    assertThat(resolution.getVoidedAt()).isEqualTo(NOW);
    verify(resolutions).saveAndFlush(resolution);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.OFFICIAL_ACT_VOIDED),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void constitutionalReviewEffectVoidsActiveElectedOfficeTerm() {
    UUID polityId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    UUID termId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion =
        motion(polityId, actor.getId(), constitution.getId(), EffectType.VOID_OFFICIAL_ACT);
    OfficeTerm term =
        new OfficeTerm(
            polityId,
            UUID.randomUUID(),
            Office.STEWARD,
            actor.getId(),
            motion.getId(),
            NOW.minusDays(1),
            NOW.plusDays(3));
    ReflectionTestUtils.setField(term, "id", termId);
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);
    ConstitutionalReviewProposal proposal =
        new ConstitutionalReviewProposal(
            polityId, motion.getId(), targetRecordId, actor.getId(), "Unconstitutional");

    when(targetRecord.getId()).thenReturn(targetRecordId);
    when(targetRecord.getPolityId()).thenReturn(polityId);
    when(targetRecord.getEntryNumber()).thenReturn(13);
    when(targetRecord.getType()).thenReturn(OfficialRecordType.OFFICE_ELECTED);
    when(targetRecord.getSourceId()).thenReturn(termId);
    when(constitutionalReviewProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionalReviewProposalProjection.class, proposal)));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));
    when(officeTerms.findEntityByIdAndPolityId(termId, polityId)).thenReturn(Optional.of(term));
    when(constitutionalReviews.saveAndFlush(any(ConstitutionalReview.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));

    effect.apply(motion, actor, constitution, NOW);

    assertThat(term.getStatus()).isEqualTo(OfficeTermStatus.ENDED);
    assertThat(term.getEndedAt()).isEqualTo(NOW);
    verify(officeTerms).saveAndFlush(term);
    verify(officialRecords)
        .append(
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(OfficialRecordType.OFFICIAL_ACT_VOIDED),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void constitutionalReviewEffectFailsWhenVoidRemedyIsUnavailable() {
    UUID polityId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    UUID resolutionId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion =
        motion(polityId, actor.getId(), constitution.getId(), EffectType.VOID_OFFICIAL_ACT);
    Resolution resolution =
        new Resolution(
            polityId, UUID.randomUUID(), "Budget resolution", "Buy more gavels.", NOW.minusDays(1));
    resolution.voidAt(NOW.minusHours(1));
    ReflectionTestUtils.setField(resolution, "id", resolutionId);
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);
    ConstitutionalReviewProposal proposal =
        new ConstitutionalReviewProposal(
            polityId, motion.getId(), targetRecordId, actor.getId(), "Unconstitutional");

    when(targetRecord.getId()).thenReturn(targetRecordId);
    when(targetRecord.getPolityId()).thenReturn(polityId);
    when(targetRecord.getType()).thenReturn(OfficialRecordType.RESOLUTION_ADOPTED);
    when(targetRecord.getSourceId()).thenReturn(resolutionId);
    when(constitutionalReviewProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionalReviewProposalProjection.class, proposal)));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));
    when(resolutions.findEntityByIdAndPolityId(resolutionId, polityId))
        .thenReturn(Optional.of(resolution));

    assertThatThrownBy(() -> effect.apply(motion, actor, constitution, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("no longer has an active remedy");

    verify(constitutionalReviews, never()).saveAndFlush(any());
    verify(officialRecords, never())
        .append(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void constitutionalReviewEffectRejectsOfficialActWithoutVoidRemedy() {
    UUID polityId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    Membership actor = member(polityId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID(), "Body");
    Motion motion =
        motion(polityId, actor.getId(), constitution.getId(), EffectType.VOID_OFFICIAL_ACT);
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);
    ConstitutionalReviewProposal proposal =
        new ConstitutionalReviewProposal(
            polityId, motion.getId(), targetRecordId, actor.getId(), "Unconstitutional");

    when(targetRecord.getType()).thenReturn(OfficialRecordType.APPEAL_GRANTED);
    when(constitutionalReviewProposals.findProjectedByMotionId(motion.getId()))
        .thenReturn(
            Optional.of(projection(ConstitutionalReviewProposalProjection.class, proposal)));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));

    assertThatThrownBy(() -> effect.apply(motion, actor, constitution, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("does not have a constitutional-review void remedy");

    verify(constitutionalReviews, never()).saveAndFlush(any());
    verify(officialRecords, never())
        .append(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  private Motion motion(
      UUID polityId, UUID actorMembershipId, UUID constitutionId, EffectType effectType) {
    Motion motion =
        new Motion(
            polityId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            constitutionId,
            UUID.randomUUID(),
            actorMembershipId,
            "Review official act",
            "Review body",
            effectType,
            NOW.minusHours(1),
            NOW.minusHours(1),
            NOW.plusHours(1),
            NOW.plusHours(1));
    ReflectionTestUtils.setField(motion, "id", UUID.randomUUID());
    return motion;
  }

  private ConstitutionVersion constitution(UUID polityId, UUID constitutionId, String body) {
    ConstitutionVersion constitution =
        new ConstitutionVersion(polityId, 1, "Starter Constitution", body, NOW.minusDays(1));
    ReflectionTestUtils.setField(constitution, "id", constitutionId);
    return constitution;
  }

  private Membership member(UUID polityId) {
    Membership member =
        new Membership(
            polityId,
            UUID.randomUUID(),
            "subject:" + UUID.randomUUID(),
            "friend@example.com",
            "Friend",
            NOW,
            null);
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private <T> T withId(T entity) {
    ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    return entity;
  }

  private static <T> T projection(Class<T> type, Object source) {
    return type.cast(
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (ignored, method, args) -> {
              if (method.getDeclaringClass() == Object.class) {
                return method.invoke(source, args);
              }
              return source.getClass().getMethod(method.getName()).invoke(source);
            }));
  }
}
