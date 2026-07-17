package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.input.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.input.CreateInstitutionChangeInput;
import com.odonta.polity.input.CreateOfficeChangeInput;
import com.odonta.polity.input.CreatePowerChangeInput;
import com.odonta.polity.input.CreateProcedureChangeInput;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionChangeOperation;
import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import com.odonta.polity.model.ConstitutionPowerChangeProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.result.ConstitutionOfficeChangeResult;
import com.odonta.polity.result.ConstitutionPowerChangeResult;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

class IntroduceConstitutionAmendmentMotionWorkflowTest extends MotionWorkflowTestFixture {
  @Test
  void createAmendmentPersistsOfficeAndPowerChanges() {
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
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Office steward = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    ConstitutionalPower admitMember =
        new ConstitutionalPower(
            polityId,
            constitution.getId(),
            PowerCode.ADMIT_MEMBER,
            "Admit citizens",
            Office.STEWARD);
    Motion[] saved = new Motion[1];
    ConstitutionAmendmentProposal[] savedProposal = new ConstitutionAmendmentProposal[1];
    List<ConstitutionOfficeChangeProposal>[] savedOfficeChanges = new List[] {List.of()};
    List<ConstitutionPowerChangeProposal>[] savedPowerChanges = new List[] {List.of()};

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(assembly));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(procedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(steward));
    when(powers.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(admitMember));
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, com.odonta.polity.model.MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor));
    when(amendmentProposals.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              savedProposal[0] = withId(invocation.getArgument(0));
              return savedProposal[0];
            });
    when(amendmentProposals.findProjectedByMotionId(any(UUID.class)))
        .thenAnswer(
            invocation ->
                Optional.of(
                    projection(
                        com.odonta.polity.repository.ConstitutionAmendmentProposalProjection.class,
                        savedProposal[0])));
    when(officeChangeProposals.saveAllAndFlush(any()))
        .thenAnswer(
            invocation -> {
              savedOfficeChanges[0] = invocation.getArgument(0);
              return savedOfficeChanges[0];
            });
    when(officeChangeProposals.findProjectionsByAmendmentProposalId(any(UUID.class)))
        .thenAnswer(
            invocation ->
                savedOfficeChanges[0].stream()
                    .map(
                        change ->
                            projection(
                                com.odonta.polity.repository
                                    .ConstitutionOfficeChangeProposalProjection.class,
                                change))
                    .toList());
    when(powerChangeProposals.saveAllAndFlush(any()))
        .thenAnswer(
            invocation -> {
              savedPowerChanges[0] = invocation.getArgument(0);
              return savedPowerChanges[0];
            });
    when(powerChangeProposals.findProjectionsByAmendmentProposalId(any(UUID.class)))
        .thenAnswer(
            invocation ->
                savedPowerChanges[0].stream()
                    .map(
                        change ->
                            projection(
                                com.odonta.polity.repository
                                    .ConstitutionPowerChangeProposalProjection.class,
                                change))
                    .toList());
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], procedure)));
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findEntitiesByMotionId(any(UUID.class))).thenReturn(List.of());

    var result =
        constitutionAmendmentMotions.introduce(
            polityId,
            new AuthenticatedUser(actorUserId, "subject", "Requester"),
            new CreateConstitutionAmendmentMotionInput(
                "Clerkship",
                "Create a clerk and give them admissions.",
                null,
                List.of(
                    new CreateOfficeChangeInput(
                        ConstitutionChangeOperation.CREATE,
                        "clerk",
                        "Clerk",
                        "Keeps the citizen roll.",
                        30,
                        1)),
                List.of(
                    new CreatePowerChangeInput(
                        PowerCode.ADMIT_MEMBER, PowerHolderScope.OFFICE, "clerk"))));

    assertThat(result.amendmentProposal().title()).isEqualTo("Clerkship");
    assertThat(result.amendmentProposal().body())
        .isEqualTo("Create a clerk and give them admissions.");
    assertThat(result.amendmentProposal().officeChanges())
        .singleElement()
        .extracting(ConstitutionOfficeChangeResult::code)
        .isEqualTo("clerk");
    assertThat(result.amendmentProposal().powerChanges())
        .singleElement()
        .extracting(ConstitutionPowerChangeResult::holderOfficeCode)
        .isEqualTo("clerk");

    verify(procedureChangeProposals).saveAllAndFlush(List.of());
    verify(institutionChangeProposals).saveAllAndFlush(List.of());
    verify(officeChangeProposals)
        .saveAllAndFlush(ArgumentMatchers.argThat(changes -> count(changes) == 1));
    verify(powerChangeProposals)
        .saveAllAndFlush(ArgumentMatchers.argThat(changes -> count(changes) == 1));
  }

  @Test
  void createAmendmentClearsElectorateOfficeWhenProcedureMovesToActiveMembers() {
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
    Procedure amendmentProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            assembly.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Procedure appealProcedure =
        new Procedure(
            polityId,
            constitution.getId(),
            court.getId(),
            Procedure.APPEAL,
            "Appeal",
            null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            2,
            0,
            24,
            EffectType.GRANT_APPEAL);
    Office magistrate =
        new Office(
            polityId,
            constitution.getId(),
            jurisdiction.getId(),
            Office.MAGISTRATE,
            "Magistrate",
            "Decides appeals.",
            14,
            3);
    Motion[] saved = new Motion[1];

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(memberships.countByPolityIdAndStatus(polityId, MembershipStatus.ACTIVE)).thenReturn(3L);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(assembly));
    when(institutions.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(assembly, court));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(amendmentProcedure));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.APPEAL))
        .thenReturn(Optional.of(appealProcedure));
    when(procedures.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(amendmentProcedure, appealProcedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(magistrate));
    when(powers.findEntitiesByConstitutionVersionId(constitution.getId())).thenReturn(List.of());
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(memberships.findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            polityId, MembershipStatus.ACTIVE))
        .thenReturn(List.of(actor));
    when(amendmentProposals.saveAndFlush(any()))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], amendmentProcedure)));
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findEntitiesByMotionId(any(UUID.class))).thenReturn(List.of());

    constitutionAmendmentMotions.introduce(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateConstitutionAmendmentMotionInput(
            "Democratize appeals",
            "Let the active membership decide appeals.",
            List.of(
                new CreateProcedureChangeInput(
                    Procedure.APPEAL,
                    null,
                    null,
                    null,
                    null,
                    ProcedureElectorate.ACTIVE_MEMBERS,
                    null,
                    null,
                    null,
                    null)),
            null,
            null));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<ConstitutionProcedureChangeProposal>> procedureChangesCaptor =
        ArgumentCaptor.forClass(Iterable.class);
    verify(procedureChangeProposals).saveAllAndFlush(procedureChangesCaptor.capture());
    List<ConstitutionProcedureChangeProposal> procedureChanges =
        StreamSupport.stream(procedureChangesCaptor.getValue().spliterator(), false).toList();
    assertThat(procedureChanges)
        .singleElement()
        .satisfies(
            change -> {
              assertThat(change.getProcedureCode()).isEqualTo(Procedure.APPEAL);
              assertThat(change.getElectorate()).isEqualTo(ProcedureElectorate.ACTIVE_MEMBERS);
              assertThat(change.getElectorateOfficeCode()).isNull();
            });
  }

  @Test
  void rejectsRetiringInstitutionStillOwningAProcedure() {
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
    Procedure amendmentProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            assembly.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(assembly));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(amendmentProcedure));
    when(institutions.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(assembly, court));
    when(jurisdictions.findEntitiesByPolityId(polityId)).thenReturn(List.of(jurisdiction));
    when(procedures.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(amendmentProcedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of());
    when(powers.findEntitiesByConstitutionVersionId(constitution.getId())).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                constitutionAmendmentMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionAmendmentMotionInput(
                        "Retire the assembly",
                        "Retire an institution without moving its procedures.",
                        List.of(
                            new CreateInstitutionChangeInput(
                                ConstitutionChangeOperation.RETIRE,
                                assembly.getId(),
                                null,
                                null,
                                null)),
                        null,
                        null,
                        null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Procedures must refer to an institution in the amended constitution.");

    verify(motions, never()).saveAndFlush(any());
  }

  @Test
  void rejectsOfficeElectionResultThresholdOnNonElectionProcedureAmendments() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure amendmentProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            institution.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Procedure ordinaryProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.ORDINARY_RESOLUTION,
            "Ordinary resolution",
            EffectType.ADOPT_RESOLUTION);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(amendmentProcedure));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.ORDINARY_RESOLUTION))
        .thenReturn(Optional.of(ordinaryProcedure));
    when(procedures.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(amendmentProcedure, ordinaryProcedure));

    assertThatThrownBy(
            () ->
                constitutionAmendmentMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionAmendmentMotionInput(
                        "Election results everywhere",
                        "Try to make ordinary motions election-result based.",
                        List.of(
                            new CreateProcedureChangeInput(
                                Procedure.ORDINARY_RESOLUTION,
                                null,
                                null,
                                VotingThreshold.OFFICE_ELECTION_RESULT,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null)),
                        null,
                        null)))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "Office-election result thresholds can only be used by office election procedures.");
  }

  @Test
  void rejectsNonElectionResultThresholdOnOfficeElectionProcedureAmendments() {
    UUID polityId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID actorMembershipId = UUID.randomUUID();
    Membership actor = member(polityId, actorUserId, actorMembershipId);
    ConstitutionVersion constitution = constitution(polityId, UUID.randomUUID());
    Jurisdiction jurisdiction = jurisdiction(polityId);
    Institution institution = institution(polityId, jurisdiction.getId(), constitution.getId());
    Procedure amendmentProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            institution.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Procedure officeElectionProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            Procedure.OFFICE_ELECTION,
            "Office election",
            EffectType.ELECT_OFFICE,
            VotingThreshold.OFFICE_ELECTION_RESULT);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(amendmentProcedure));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.OFFICE_ELECTION))
        .thenReturn(Optional.of(officeElectionProcedure));
    when(procedures.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(amendmentProcedure, officeElectionProcedure));

    assertThatThrownBy(
            () ->
                constitutionAmendmentMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionAmendmentMotionInput(
                        "Office election supermajority",
                        "Try to make office elections use a yes/no threshold.",
                        List.of(
                            new CreateProcedureChangeInput(
                                Procedure.OFFICE_ELECTION,
                                null,
                                null,
                                VotingThreshold.SIMPLE_MAJORITY_CAST,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null)),
                        null,
                        null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Office election procedures must use office-election result thresholds.");
  }

  @Test
  void rejectsAmendmentsThatMoveDisbandmentOutOfCitizenHands() {
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
            institution.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Office steward = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    ConstitutionalPower disbandment =
        new ConstitutionalPower(
            polityId,
            constitution.getId(),
            PowerCode.INTRODUCE_DISBANDMENT,
            "Propose disbandment",
            PowerHolderScope.ACTIVE_MEMBER);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(procedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(steward));
    when(powers.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(disbandment));

    assertThatThrownBy(
            () ->
                constitutionAmendmentMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionAmendmentMotionInput(
                        "Gate elections",
                        "Try to move disbandment to the Steward.",
                        null,
                        null,
                        List.of(
                            new CreatePowerChangeInput(
                                PowerCode.INTRODUCE_DISBANDMENT,
                                PowerHolderScope.OFFICE,
                                Office.STEWARD)))))
        .isInstanceOf(ApiException.class)
        .hasMessage("Disbandment must remain an active citizen power.");

    verify(motions, never()).saveAndFlush(any());
  }

  @Test
  void rejectsRetiringOfficeStillHoldingAPower() {
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
            institution.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Office steward = stewardOffice(polityId, constitution.getId(), jurisdiction.getId());
    ConstitutionalPower admitMember =
        new ConstitutionalPower(
            polityId,
            constitution.getId(),
            PowerCode.ADMIT_MEMBER,
            "Admit citizens",
            Office.STEWARD);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(institution));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(procedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(steward));
    when(powers.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(admitMember));

    assertThatThrownBy(
            () ->
                constitutionAmendmentMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionAmendmentMotionInput(
                        "Retire steward",
                        "Retire the steward without moving powers.",
                        null,
                        List.of(
                            new CreateOfficeChangeInput(
                                ConstitutionChangeOperation.RETIRE,
                                Office.STEWARD,
                                null,
                                null,
                                null,
                                null)),
                        null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Office-held powers must refer to an office in the amended constitution.");
  }

  @Test
  void rejectsRetiringOfficeStillDecidingAProcedure() {
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
    Procedure amendmentProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            assembly.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Procedure appealProcedure =
        new Procedure(
            polityId,
            constitution.getId(),
            court.getId(),
            Procedure.APPEAL,
            "Appeal",
            null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            0,
            24,
            EffectType.GRANT_APPEAL);
    Office magistrate =
        new Office(
            polityId,
            constitution.getId(),
            jurisdiction.getId(),
            Office.MAGISTRATE,
            "Magistrate",
            "Decides appeals.",
            14);

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(assembly));
    when(institutions.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(assembly, court));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(amendmentProcedure));
    when(procedures.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(amendmentProcedure, appealProcedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(magistrate));
    when(powers.findEntitiesByConstitutionVersionId(constitution.getId())).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                constitutionAmendmentMotions.introduce(
                    polityId,
                    new AuthenticatedUser(actorUserId, "subject", "Requester"),
                    new CreateConstitutionAmendmentMotionInput(
                        "Retire court",
                        "Retire the court without moving appeals.",
                        null,
                        List.of(
                            new CreateOfficeChangeInput(
                                ConstitutionChangeOperation.RETIRE,
                                Office.MAGISTRATE,
                                null,
                                null,
                                null,
                                null)),
                        null)))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "Office-held procedure electorates must refer to an office in the amended"
                + " constitution.");
  }

  @Test
  void allowsReducingOfficeSeatsBelowProcedureMinimumElectorate() {
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
    Procedure amendmentProcedure =
        procedure(
            polityId,
            UUID.randomUUID(),
            constitution.getId(),
            assembly.getId(),
            Procedure.CONSTITUTION_AMENDMENT,
            "Constitutional amendment",
            EffectType.AMEND_CONSTITUTION);
    Procedure constitutionalReviewProcedure =
        new Procedure(
            polityId,
            constitution.getId(),
            court.getId(),
            Procedure.CONSTITUTIONAL_REVIEW,
            "Constitutional review",
            null,
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY_CAST,
            ProcedureElectorate.OFFICE_HOLDERS,
            Office.MAGISTRATE,
            2,
            0,
            24,
            EffectType.VOID_OFFICIAL_ACT);
    Office magistrate =
        new Office(
            polityId,
            constitution.getId(),
            jurisdiction.getId(),
            Office.MAGISTRATE,
            "Magistrate",
            "Decides reviews.",
            14,
            3);
    Motion[] saved = new Motion[1];

    when(activeMemberships.resolve(polityId, actorUserId)).thenReturn(actor);
    when(constitutions.findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED))
        .thenReturn(Optional.of(constitution));
    when(jurisdictions.findEntityByPolityIdAndKind(polityId, JurisdictionKind.ROOT))
        .thenReturn(Optional.of(jurisdiction));
    when(institutions.findEntityByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenReturn(Optional.of(assembly));
    when(institutions.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(assembly, court));
    when(procedures.findEntityByConstitutionVersionIdAndCode(
            constitution.getId(), Procedure.CONSTITUTION_AMENDMENT))
        .thenReturn(Optional.of(amendmentProcedure));
    when(procedures.findEntitiesByConstitutionVersionId(constitution.getId()))
        .thenReturn(List.of(amendmentProcedure, constitutionalReviewProcedure));
    when(offices.findEntitiesByConstitutionVersionIdOrderByName(constitution.getId()))
        .thenReturn(List.of(magistrate));
    when(powers.findEntitiesByConstitutionVersionId(constitution.getId())).thenReturn(List.of());
    when(motions.saveAndFlush(any(Motion.class)))
        .thenAnswer(
            invocation -> {
              saved[0] = withId(invocation.getArgument(0));
              return saved[0];
            });
    when(procedureElectorates.electors(eq(amendmentProcedure), any(OffsetDateTime.class)))
        .thenReturn(List.of(actor));
    when(amendmentProposals.saveAndFlush(any()))
        .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(motions.findProjectedByIdAndPolityId(any(UUID.class), eq(polityId)))
        .thenAnswer(invocation -> Optional.of(projection(saved[0], amendmentProcedure)));
    when(electors.countByMotionId(any(UUID.class))).thenReturn(1L);
    when(votes.findEntitiesByMotionId(any(UUID.class))).thenReturn(List.of());

    constitutionAmendmentMotions.introduce(
        polityId,
        new AuthenticatedUser(actorUserId, "subject", "Requester"),
        new CreateConstitutionAmendmentMotionInput(
            "Shrink the bench",
            "Reduce the court below its own review minimum.",
            null,
            List.of(
                new CreateOfficeChangeInput(
                    ConstitutionChangeOperation.REVISE, Office.MAGISTRATE, null, null, null, 1)),
            null));

    verify(motions).saveAndFlush(any());
    verify(officeChangeProposals).saveAllAndFlush(any());
  }
}
