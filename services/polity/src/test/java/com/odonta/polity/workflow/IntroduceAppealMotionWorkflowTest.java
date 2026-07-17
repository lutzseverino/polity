package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.CreateAppealMotionInput;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionElector;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.repository.AppealProposalProjection;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

class IntroduceAppealMotionWorkflowTest extends MotionWorkflowTestFixture {
  @Test
  void createAppealUsesTheAppealProcedureEffectType() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    UUID sanctionIntroducerId = UUID.randomUUID();
    Sanction sanction = sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.plusDays(7));
    Motion[] saved = new Motion[1];

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(procedure));
    when(sanctions.findEntityByIdAndPolityId(sanction.getId(), polityId))
        .thenReturn(Optional.of(sanction));
    stubSanctionMotion(polityId, sanction, sanctionIntroducerId);
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, com.odonta.polity.model.MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findEntitiesByMotionId(any(UUID.class))).thenReturn(List.of());

    appealMotions.introduce(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateAppealMotionInput(sanction.getId(), "Fresh evidence"));

    ArgumentCaptor<Motion> motionCaptor = ArgumentCaptor.forClass(Motion.class);
    verify(motions).saveAndFlush(motionCaptor.capture());
    assertThat(motionCaptor.getValue().getEffectType()).isEqualTo(EffectType.GRANT_APPEAL);
    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_APPEAL);
  }

  @Test
  void createAppealRecusesSanctionTargetAppellantAndSanctionIntroducer() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    UUID targetMembershipId = UUID.randomUUID();
    UUID sanctionIntroducerId = UUID.randomUUID();
    UUID magistrateMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    Membership target = member(polityId, UUID.randomUUID(), targetMembershipId);
    Membership sanctionIntroducer = member(polityId, UUID.randomUUID(), sanctionIntroducerId);
    Membership magistrate = member(polityId, UUID.randomUUID(), magistrateMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    Sanction sanction = sanction(polityId, UUID.randomUUID(), targetMembershipId, NOW.plusDays(7));
    Motion[] saved = new Motion[1];

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(procedure));
    when(sanctions.findEntityByIdAndPolityId(sanction.getId(), polityId))
        .thenReturn(Optional.of(sanction));
    stubSanctionMotion(polityId, sanction, sanctionIntroducerId);
    when(procedureElectorates.electors(eq(procedure), any(OffsetDateTime.class)))
        .thenReturn(List.of(actor, target, sanctionIntroducer, magistrate));
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findEntitiesByMotionId(any(UUID.class))).thenReturn(List.of());

    appealMotions.introduce(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateAppealMotionInput(sanction.getId(), "Fresh evidence"));

    verify(electors)
        .saveAllAndFlush(
            ArgumentMatchers.argThat(
                savedElectors -> {
                  MotionElector only = savedElectors.iterator().next();
                  return count(savedElectors) == 1
                      && only.getMembershipId().equals(magistrateMembershipId);
                }));
  }

  @Test
  void createOwnAppealBypassesStandingOnlyForTheSanctionedMember() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    UUID sanctionIntroducerId = UUID.randomUUID();
    Sanction sanction = sanction(polityId, UUID.randomUUID(), actorMembershipId, NOW.plusDays(7));
    Motion[] saved = new Motion[1];

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(procedure));
    when(sanctions.findEntityByIdAndPolityId(sanction.getId(), polityId))
        .thenReturn(Optional.of(sanction));
    stubSanctionMotion(polityId, sanction, sanctionIntroducerId);
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, com.odonta.polity.model.MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findEntitiesByMotionId(any(UUID.class))).thenReturn(List.of());

    appealMotions.introduce(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateAppealMotionInput(sanction.getId(), "Fresh evidence"));

    verify(authority).requireOwnAppealIntroduction(actor, constitution);
    verify(authority, never()).require(actor, constitution, PowerCode.INTRODUCE_APPEAL);
  }

  @Test
  void rejectsAppealsForExpiredSanctions() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    Sanction sanction =
        sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.minusMinutes(1));
    stubAppealCreationContext(polityId, actorUserId, sanction);

    assertThatThrownBy(
            () ->
                appealMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateAppealMotionInput(sanction.getId(), "Too late")))
        .isInstanceOf(ApiException.class)
        .hasMessage("Only active sanctions can be appealed.");
  }

  @Test
  void rejectsDuplicateGrantedAppeals() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    Sanction sanction = sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.plusDays(7));
    stubAppealCreationContext(polityId, actorUserId, sanction);
    when(appeals.existsByPolityIdAndSanctionId(polityId, sanction.getId())).thenReturn(true);

    assertThatThrownBy(
            () ->
                appealMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateAppealMotionInput(sanction.getId(), "Already handled")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This sanction has already been appealed.");
  }

  @Test
  void rejectsDuplicateOpenAppeals() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    Sanction sanction = sanction(polityId, UUID.randomUUID(), UUID.randomUUID(), NOW.plusDays(7));
    stubAppealCreationContext(polityId, actorUserId, sanction);
    UUID openMotionId = UUID.randomUUID();
    AppealProposal proposal =
        new AppealProposal(
            polityId, openMotionId, sanction.getId(), UUID.randomUUID(), "Already open");
    when(appealProposals.findProjectionsByPolityIdAndSanctionId(polityId, sanction.getId()))
        .thenReturn(List.of(projection(AppealProposalProjection.class, proposal)));
    when(motions.existsByIdAndStatus(openMotionId, MotionStatus.VOTING)).thenReturn(true);

    assertThatThrownBy(
            () ->
                appealMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateAppealMotionInput(sanction.getId(), "Already open")))
        .isInstanceOf(ApiException.class)
        .hasMessage("This sanction already has an open appeal motion.");
  }
}
