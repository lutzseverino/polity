package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.CreateMotionInput;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IntroduceMotionWorkflowTest extends MotionWorkflowTestFixture {
  @Test
  void rejectsGoverningMotionsWhenProcedureElectorateIsBelowMinimum() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution assembly = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.ORDINARY_RESOLUTION,
            "Ordinary resolution",
            EffectType.ADOPT_RESOLUTION);
    ReflectionTestUtils.setField(procedure, "minimumElectorCount", 2);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(assembly));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.ORDINARY_RESOLUTION))
        .thenReturn(Optional.of(procedure));
    when(procedureElectorates.electors(eq(procedure), any(OffsetDateTime.class)))
        .thenReturn(List.of(actor));

    assertThatThrownBy(
            () ->
                introduceMotion.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateMotionInput("Paint the plaza", "Ceremonially, but enforceably.")))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "This procedure does not have enough eligible electors under the current"
                + " constitution.");

    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_MOTION);
    verify(motions, never()).saveAndFlush(any());
  }
}
