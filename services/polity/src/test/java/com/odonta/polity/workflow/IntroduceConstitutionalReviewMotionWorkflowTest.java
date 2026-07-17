package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.CreateConstitutionalReviewMotionInput;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionElector;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Sanction;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

class IntroduceConstitutionalReviewMotionWorkflowTest extends MotionWorkflowTestFixture {
  @Test
  void createConstitutionalReviewPersistsProposalAndRecusesInterestedMembers() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    UUID targetMembershipId = UUID.randomUUID();
    UUID sanctionIntroducerId = UUID.randomUUID();
    UUID magistrateMembershipId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    Membership target = member(polityId, UUID.randomUUID(), targetMembershipId);
    Membership sanctionIntroducer = member(polityId, UUID.randomUUID(), sanctionIntroducerId);
    Membership magistrate = member(polityId, UUID.randomUUID(), magistrateMembershipId);
    Sanction sanction = sanction(polityId, UUID.randomUUID(), targetMembershipId, NOW.plusDays(7));
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.CONSTITUTIONAL_REVIEW,
            "Constitutional review",
            EffectType.VOID_OFFICIAL_ACT);
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);
    Motion[] saved = new Motion[1];

    when(targetRecord.getId()).thenReturn(targetRecordId);
    when(targetRecord.getPolityId()).thenReturn(polityId);
    when(targetRecord.getActorMembershipId()).thenReturn(actorMembershipId);
    when(targetRecord.getEntryNumber()).thenReturn(12);
    when(targetRecord.getType()).thenReturn(OfficialRecordType.SANCTION_APPLIED);
    when(targetRecord.getSourceId()).thenReturn(sanction.getId());
    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));
    when(sanctions.findEntityByIdAndPolityId(sanction.getId(), polityId))
        .thenReturn(Optional.of(sanction));
    stubSanctionMotion(polityId, sanction, sanctionIntroducerId);
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTIONAL_REVIEW))
        .thenReturn(Optional.of(procedure));
    when(procedureElectorates.electors(any(Procedure.class), any(OffsetDateTime.class)))
        .thenReturn(List.of(actor, target, sanctionIntroducer, magistrate));
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(constitutionalReviewProposals.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findEntitiesByMotionId(any(UUID.class))).thenReturn(List.of());

    var result =
        constitutionalReviewMotions.introduce(
            polityId,
            new AuthenticatedUser(actorUserId, "subject", "Requester"),
            new CreateConstitutionalReviewMotionInput(targetRecordId, "Wrong authority"));

    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW);
    verify(constitutionalReviewProposals).saveAndFlush(any());
    verify(electors)
        .saveAllAndFlush(
            ArgumentMatchers.argThat(
                savedElectors -> {
                  MotionElector only = savedElectors.iterator().next();
                  return count(savedElectors) == 1
                      && only.getMembershipId().equals(magistrateMembershipId);
                }));
    assertThat(result.effectType()).isEqualTo(EffectType.VOID_OFFICIAL_ACT);
    assertThat(saved[0].getTitleKey()).isEqualTo("motion.constitutional_review.title");
    assertThat(saved[0].getTemplateParams()).containsEntry("entryNumber", 12);
  }

  @Test
  void createConstitutionalReviewRejectsOfficialActWithoutVoidRemedy() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);

    when(targetRecord.getType()).thenReturn(OfficialRecordType.MEMBER_ADMITTED);
    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));

    assertThatThrownBy(
            () ->
                constitutionalReviewMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionalReviewMotionInput(targetRecordId, "Wrong authority")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This official act does not have a constitutional-review void remedy.");

    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW);
    verify(motions, never()).saveAndFlush(any());
    verify(constitutionalReviewProposals, never()).saveAndFlush(any());
  }

  @Test
  void createConstitutionalReviewRejectsOfficialActAlreadyReviewed() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);

    when(targetRecord.getId()).thenReturn(targetRecordId);
    when(targetRecord.getType()).thenReturn(OfficialRecordType.RESOLUTION_ADOPTED);
    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));
    when(constitutionalReviews.existsByPolityIdAndTargetRecordId(polityId, targetRecordId))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                constitutionalReviewMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionalReviewMotionInput(targetRecordId, "Wrong authority")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This official act has already been constitutionally reviewed.");

    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW);
    verify(motions, never()).saveAndFlush(any());
    verify(constitutionalReviewProposals, never()).saveAndFlush(any());
  }

  @Test
  void createConstitutionalReviewRejectsOfficialActWithoutActiveVoidRemedy() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    UUID targetMembershipId = UUID.randomUUID();
    UUID sanctionId = UUID.randomUUID();
    UUID targetRecordId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Sanction sanction =
        sanction(polityId, UUID.randomUUID(), targetMembershipId, NOW.minusMinutes(1));
    ReflectionTestUtils.setField(sanction, "id", sanctionId);
    OfficialRecordEntry targetRecord = mock(OfficialRecordEntry.class);

    when(targetRecord.getId()).thenReturn(targetRecordId);
    when(targetRecord.getPolityId()).thenReturn(polityId);
    when(targetRecord.getType()).thenReturn(OfficialRecordType.SANCTION_APPLIED);
    when(targetRecord.getSourceId()).thenReturn(sanctionId);
    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(officialRecordEntries.findEntityByIdAndPolityId(targetRecordId, polityId))
        .thenReturn(Optional.of(targetRecord));
    when(sanctions.findEntityByIdAndPolityId(sanctionId, polityId))
        .thenReturn(Optional.of(sanction));

    assertThatThrownBy(
            () ->
                constitutionalReviewMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionalReviewMotionInput(targetRecordId, "Wrong authority")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This official act no longer has an active remedy to void.");

    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW);
    verify(motions, never()).saveAndFlush(any());
    verify(constitutionalReviewProposals, never()).saveAndFlush(any());
  }
}
