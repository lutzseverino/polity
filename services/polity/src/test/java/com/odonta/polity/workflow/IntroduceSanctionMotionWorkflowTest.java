package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.CreateSanctionMotionInput;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.result.ActionAvailabilityResult;
import com.odonta.polity.result.ActionUnavailableReason;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IntroduceSanctionMotionWorkflowTest extends MotionWorkflowTestFixture {
  @Test
  void rejectsSanctionsWhenAppealProcedureIsUnavailable() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID targetMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, UUID.randomUUID());
    Membership target = member(polityId, UUID.randomUUID(), targetMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(memberships.findEntityById(targetMembershipId)).thenReturn(Optional.of(target));
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(polityActionAvailability.sanctionAvailability(actor.getPolityId(), actor.getUserId()))
        .thenReturn(
            ActionAvailabilityResult.blocked(ActionUnavailableReason.APPEAL_PROCEDURE_UNAVAILABLE));

    assertThatThrownBy(
            () ->
                sanctionMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Tribune"),
                    new CreateSanctionMotionInput(
                        targetMembershipId, SanctionType.SUSPENSION, "Missing appeal court", 7)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Sanctions require an available appeal procedure.");

    verify(motions, never()).saveAndFlush(any());
  }

  @Test
  void rejectsSanctionsWhenTargetRecusalBreaksAppealProcedure() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID targetMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, UUID.randomUUID());
    Membership target = member(polityId, UUID.randomUUID(), targetMembershipId);
    Membership otherMagistrate = member(polityId, UUID.randomUUID(), UUID.randomUUID());
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Procedure appealProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    ReflectionTestUtils.setField(appealProcedure, "electorate", ProcedureElectorate.OFFICE_HOLDERS);
    ReflectionTestUtils.setField(appealProcedure, "electorateOfficeCode", Office.MAGISTRATE);
    ReflectionTestUtils.setField(appealProcedure, "minimumElectorCount", 2);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(memberships.findEntityById(targetMembershipId)).thenReturn(Optional.of(target));
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(polityActionAvailability.sanctionAvailability(actor.getPolityId(), actor.getUserId()))
        .thenReturn(ActionAvailabilityResult.allowed());
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(appealProcedure));
    when(procedureElectorates.electors(eq(appealProcedure), any(OffsetDateTime.class)))
        .thenReturn(List.of(target, otherMagistrate));

    assertThatThrownBy(
            () ->
                sanctionMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Tribune"),
                    new CreateSanctionMotionInput(
                        targetMembershipId, SanctionType.SUSPENSION, "Thin bench", 7)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Sanctions require an available appeal procedure after conflict recusal.");

    verify(motions, never()).saveAndFlush(any());
  }

  @Test
  void rejectsSanctionsWhenIntroducerRecusalBreaksAppealProcedure() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID targetMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, UUID.randomUUID());
    Membership target = member(polityId, UUID.randomUUID(), targetMembershipId);
    Membership otherMagistrate = member(polityId, UUID.randomUUID(), UUID.randomUUID());
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Procedure appealProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    ReflectionTestUtils.setField(appealProcedure, "electorate", ProcedureElectorate.OFFICE_HOLDERS);
    ReflectionTestUtils.setField(appealProcedure, "electorateOfficeCode", Office.MAGISTRATE);
    ReflectionTestUtils.setField(appealProcedure, "minimumElectorCount", 2);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(memberships.findEntityById(targetMembershipId)).thenReturn(Optional.of(target));
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(polityActionAvailability.sanctionAvailability(actor.getPolityId(), actor.getUserId()))
        .thenReturn(ActionAvailabilityResult.allowed());
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(appealProcedure));
    when(procedureElectorates.electors(eq(appealProcedure), any(OffsetDateTime.class)))
        .thenReturn(List.of(actor, otherMagistrate));

    assertThatThrownBy(
            () ->
                sanctionMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Tribune"),
                    new CreateSanctionMotionInput(
                        targetMembershipId, SanctionType.SUSPENSION, "Conflicted bench", 7)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Sanctions require an available appeal procedure after conflict recusal.");

    verify(motions, never()).saveAndFlush(any());
  }

  @Test
  void rejectsSanctionsTooShortForAppealProcedureToComplete() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID targetMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, UUID.randomUUID());
    Membership target = member(polityId, UUID.randomUUID(), targetMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Procedure appealProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.APPEAL,
            "Appeal",
            EffectType.GRANT_APPEAL);
    ReflectionTestUtils.setField(appealProcedure, "votingPeriodHours", 48);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(memberships.findEntityById(targetMembershipId)).thenReturn(Optional.of(target));
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(polityActionAvailability.sanctionAvailability(actor.getPolityId(), actor.getUserId()))
        .thenReturn(ActionAvailabilityResult.allowed());
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(appealProcedure));

    assertThatThrownBy(
            () ->
                sanctionMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Tribune"),
                    new CreateSanctionMotionInput(
                        targetMembershipId, SanctionType.SUSPENSION, "Too short", 2)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Sanctions must last long enough for an appeal to be completed.")
        .satisfies(
            exception ->
                assertThat(((ApiException) exception).details())
                    .containsEntry("minimumDurationDays", 3));

    verify(motions, never()).saveAndFlush(any());
  }
}
