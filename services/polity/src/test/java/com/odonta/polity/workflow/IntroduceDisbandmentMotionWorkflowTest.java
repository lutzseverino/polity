package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.CreateDisbandmentMotionInput;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.VotingThreshold;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class IntroduceDisbandmentMotionWorkflowTest extends MotionWorkflowTestFixture {
  @Test
  void createDisbandmentUsesConstitutionalProcedureAndPower() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution assembly = institution(polityId, jurisdiction.getId(), constitution.getId());
    Institution court =
        new Institution(
            polityId,
            jurisdiction.getId(),
            constitution.getId(),
            "Magistrates' Court",
            InstitutionKind.JUDICIARY);
    ReflectionTestUtils.setField(court, "id", UUID.randomUUID());
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.DISBANDMENT,
            "Disbandment",
            EffectType.DISBAND_POLITY,
            VotingThreshold.TWO_THIRDS_ELIGIBLE);
    Motion[] saved = new Motion[1];

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(assembly));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.DISBANDMENT))
        .thenReturn(Optional.of(procedure));
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

    disbandmentMotions.introduce(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateDisbandmentMotionInput("End the council", "Archive the polity."));

    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_DISBANDMENT);
    verify(polities).requireDisbandmentGovernment(polityId);
    ArgumentCaptor<Motion> motionCaptor = ArgumentCaptor.forClass(Motion.class);
    verify(motions).saveAndFlush(motionCaptor.capture());
    assertThat(motionCaptor.getValue().getEffectType()).isEqualTo(EffectType.DISBAND_POLITY);
  }

  @Test
  void rejectsDisbandmentWhenGovernmentUnavailable() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    doThrow(
            ApiException.conflict(
                "polity_provisional",
                "This polity needs at least three citizens with political standing before full"
                    + " government motions can be introduced."))
        .when(polities)
        .requireDisbandmentGovernment(polityId);

    assertThatThrownBy(
            () ->
                disbandmentMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateDisbandmentMotionInput("End the council", "Archive the polity.")))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "This polity needs at least three citizens with political standing before full"
                + " government motions can be introduced.");

    verify(motions, never()).saveAndFlush(any());
  }
}
