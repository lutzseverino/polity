package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.CreateOfficeTermReviewMotionInput;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionElector;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

class IntroduceOfficeTermReviewMotionWorkflowTest extends MotionWorkflowTestFixture {
  @Test
  void createOfficeTermReviewPersistsProposalAndRecusesInterestedMagistrates() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    UUID targetMembershipId = UUID.randomUUID();
    UUID magistrateMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    Membership target = member(polityId, UUID.randomUUID(), targetMembershipId);
    Membership magistrate = member(polityId, UUID.randomUUID(), magistrateMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Office office = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    OfficeTerm term =
        officeTerm(polityId, office.getId(), office.getCode(), targetMembershipId, NOW.plusDays(7));
    Procedure procedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.OFFICE_TERM_REVIEW,
            "Office term review",
            EffectType.VACATE_OFFICE_TERM);
    Motion[] saved = new Motion[1];

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(membershipService.displayName(targetMembershipId)).thenReturn(target.getDisplayName());
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(officeTerms.findEntityByIdAndPolityId(term.getId(), polityId))
        .thenReturn(Optional.of(term));
    when(offices.findEntityByIdAndPolityId(office.getId(), polityId))
        .thenReturn(Optional.of(office));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.OFFICE_TERM_REVIEW))
        .thenReturn(Optional.of(procedure));
    when(procedureElectorates.electors(any(Procedure.class), any(OffsetDateTime.class)))
        .thenReturn(List.of(actor, target, magistrate));
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(officeTermReviewProposals.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findEntitiesByMotionId(any(UUID.class))).thenReturn(List.of());

    var result =
        officeTermReviewMotions.introduce(
            polityId,
            new AuthenticatedUser(actorUserId, "subject", "Requester"),
            new CreateOfficeTermReviewMotionInput(term.getId(), "Contested authority"));

    verify(authority).require(actor, constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW);
    verify(officeTermReviewProposals).saveAndFlush(any());
    verify(electors)
        .saveAllAndFlush(
            ArgumentMatchers.argThat(
                savedElectors -> {
                  MotionElector only = savedElectors.iterator().next();
                  return count(savedElectors) == 1
                      && only.getMembershipId().equals(magistrateMembershipId);
                }));
    assertThat(result.effectType()).isEqualTo(EffectType.VACATE_OFFICE_TERM);
    assertThat(saved[0].getTitleKey()).isEqualTo("motion.office_term_review.title");
    assertThat(saved[0].getTemplateParams()).containsEntry("officeName", office.getName());
  }

  @Test
  void rejectsJudicialReviewWhenRecusalsLeaveTooFewElectors() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    UUID targetMembershipId = UUID.randomUUID();
    UUID magistrateMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    Membership target = member(polityId, UUID.randomUUID(), targetMembershipId);
    Membership magistrate = member(polityId, UUID.randomUUID(), magistrateMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Office office = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    OfficeTerm term =
        officeTerm(polityId, office.getId(), office.getCode(), targetMembershipId, NOW.plusDays(7));
    Procedure procedure =
        new Procedure(
            polityId,
            constitution.getId(),
            UUID.randomUUID(),
            Procedure.OFFICE_TERM_REVIEW,
            "Office term review",
            (com.odonta.polity.model.ProcedureTemplateKey) null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            2,
            0,
            24,
            EffectType.VACATE_OFFICE_TERM);
    ReflectionTestUtils.setField(procedure, "id", UUID.randomUUID());

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(membershipService.displayName(targetMembershipId)).thenReturn(target.getDisplayName());
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(officeTerms.findEntityByIdAndPolityId(term.getId(), polityId))
        .thenReturn(Optional.of(term));
    when(offices.findEntityByIdAndPolityId(office.getId(), polityId))
        .thenReturn(Optional.of(office));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.OFFICE_TERM_REVIEW))
        .thenReturn(Optional.of(procedure));
    when(procedureElectorates.electors(any(Procedure.class), any(OffsetDateTime.class)))
        .thenReturn(List.of(actor, target, magistrate));

    assertThatThrownBy(
            () ->
                officeTermReviewMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateOfficeTermReviewMotionInput(term.getId(), "Contested authority")))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "This procedure does not have enough eligible electors under the current"
                + " constitution.");

    verify(motions, never()).saveAndFlush(any());
  }
}
