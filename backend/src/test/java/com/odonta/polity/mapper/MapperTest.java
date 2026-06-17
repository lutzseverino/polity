package com.odonta.polity.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.api.model.CastVoteRequest;
import com.odonta.polity.api.model.MemberResponse;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.model.AppealStatus;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.InvitationStatus;
import com.odonta.polity.model.MembershipInvitationResult;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProjection;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficialRecordProjection;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.SanctionProjection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

class MapperTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-13T10:00:00Z");

  private final PolityApplicationMapper polityApplication =
      Mappers.getMapper(PolityApplicationMapper.class);
  private final PolityTransportMapper polityTransport =
      Mappers.getMapper(PolityTransportMapper.class);
  private final MotionApplicationMapper motionApplication =
      Mappers.getMapper(MotionApplicationMapper.class);
  private final MotionTransportMapper motionTransport =
      Mappers.getMapper(MotionTransportMapper.class);
  private final OfficeApplicationMapper officeApplication =
      Mappers.getMapper(OfficeApplicationMapper.class);
  private final OfficeTransportMapper officeTransport =
      Mappers.getMapper(OfficeTransportMapper.class);
  private final JusticeApplicationMapper justiceApplication =
      Mappers.getMapper(JusticeApplicationMapper.class);
  private final JusticeTransportMapper justiceTransport =
      Mappers.getMapper(JusticeTransportMapper.class);
  private final OfficialRecordApplicationMapper recordApplication =
      Mappers.getMapper(OfficialRecordApplicationMapper.class);
  private final OfficialRecordTransportMapper recordTransport =
      Mappers.getMapper(OfficialRecordTransportMapper.class);
  private final InvitationTransportMapper invitationTransport =
      Mappers.getMapper(InvitationTransportMapper.class);

  MapperTest() {
    ReflectionTestUtils.setField(
        motionTransport, "motionTransportConversions", new MotionTransportConversions());
    ReflectionTestUtils.setField(
        recordTransport,
        "officialRecordResponseConversions",
        new OfficialRecordResponseConversions());
  }

  @Test
  void mapsPolityAndMemberAcrossApplicationAndTransportBoundaries() {
    UUID polityId = UUID.randomUUID();
    PolityProjection polity = mock(PolityProjection.class);
    when(polity.getId()).thenReturn(polityId);
    when(polity.getName()).thenReturn("Civic Assembly");
    when(polity.getVisibility()).thenReturn(PolityVisibility.PUBLIC);
    when(polity.getConstitutionVersion()).thenReturn(3);
    when(polity.getJurisdictionName()).thenReturn("Commons");
    when(polity.getInstitutionName()).thenReturn("Assembly");
    when(polity.getCreatedAt()).thenReturn(NOW);

    var polityResponse = polityTransport.toResponse(polityApplication.toResult(polity));

    assertThat(polityResponse.getId()).isEqualTo(polityId);
    assertThat(polityResponse.getVisibility())
        .isEqualTo(com.odonta.polity.api.model.PolityVisibility.PUBLIC);
    assertThat(polityResponse.getConstitutionVersion()).isEqualTo(3);
    assertThat(polityResponse.getJurisdictionName()).isEqualTo("Commons");

    var createRequest = new com.odonta.polity.api.model.CreatePolityRequest();
    createRequest.setName("Civic Assembly");
    createRequest.setVisibility(com.odonta.polity.api.model.PolityVisibility.PUBLIC);

    assertThat(polityTransport.toInput(createRequest).visibility())
        .isEqualTo(PolityVisibility.PUBLIC);

    MembershipProjection member = mock(MembershipProjection.class);
    when(member.getName()).thenReturn("Ada");
    when(member.getStatus()).thenReturn(MembershipStatus.ACTIVE);

    MemberResponse memberResponse = polityTransport.toResponse(polityApplication.toResult(member));

    assertThat(memberResponse.getName()).isEqualTo("Ada");
    assertThat(memberResponse.getStatus()).isEqualTo(MemberResponse.StatusEnum.ACTIVE);
    assertThat(
            polityTransport.toMemberResponses(polityApplication.toMemberResults(List.of(member))))
        .hasSize(1);
  }

  @Test
  void mapsMotionAndVoteAcrossApplicationAndTransportBoundaries() {
    MotionProjection motion = mock(MotionProjection.class);
    Certification certification = mock(Certification.class);
    when(motion.getStatus()).thenReturn(MotionStatus.VOTING);
    when(motion.getEffectType()).thenReturn(EffectType.ADOPT_RESOLUTION);
    when(motion.getProcedureName()).thenReturn("Ordinary resolution");
    when(motion.getThreshold()).thenReturn(VotingThreshold.SIMPLE_MAJORITY_CAST);
    when(motion.getConstitutionVersion()).thenReturn(1);
    when(motion.getIntroducedByName()).thenReturn("Ada");
    when(motion.getOpenedAt()).thenReturn(NOW);
    when(motion.getVotingOpensAt()).thenReturn(NOW);
    when(motion.getVotingClosesAt()).thenReturn(NOW.plusHours(24));
    when(motion.getCertificationOpensAt()).thenReturn(NOW.plusHours(24));
    when(certification.isPassed()).thenReturn(true);
    when(certification.getExplanation()).thenReturn("The motion passed.");
    when(certification.getCertifiedAt()).thenReturn(NOW);
    VotingResult tally = new VotingResult(5, 3, 1, 1, 3, true, true, true, "Passed");

    MotionResponse response =
        motionTransport.toResponse(motionApplication.toResult(motion, tally, certification));

    assertThat(response.getStatus()).isEqualTo(MotionResponse.StatusEnum.VOTING);
    assertThat(response.getEffectType())
        .isEqualTo(com.odonta.polity.api.model.EffectType.ADOPT_RESOLUTION);
    assertThat(response.getTally().getYes()).isEqualTo(3);
    assertThat(response.getCertification().getPassed()).isTrue();

    var voteRequest = new CastVoteRequest(com.odonta.polity.api.model.VoteChoice.NO);
    assertThat(motionTransport.toInput(voteRequest).choice()).isEqualTo(VoteChoice.NO);
  }

  @Test
  void mapsOfficialRecordAcrossApplicationAndTransportBoundaries() {
    OfficialRecordProjection record = mock(OfficialRecordProjection.class);
    when(record.getEntryNumber()).thenReturn(12);
    when(record.getType()).thenReturn(OfficialRecordType.CONSTITUTION_RATIFIED);
    when(record.getActorName()).thenReturn("Ada");
    when(record.getConstitutionVersion()).thenReturn(2);
    when(record.getPowerCode()).thenReturn(PowerCode.REQUEST_CERTIFICATION);
    when(record.getEffectType()).thenReturn(EffectType.AMEND_CONSTITUTION);
    when(record.getOutcome()).thenReturn("passed");

    var response = recordTransport.toResponse(recordApplication.toResult(record));

    assertThat(response.getType())
        .isEqualTo(com.odonta.polity.api.model.OfficialRecordType.CONSTITUTION_RATIFIED);
    assertThat(response.getEntryNumber()).isEqualTo(12);
    assertThat(response.getPowerCode())
        .isEqualTo(com.odonta.polity.api.model.PowerCode.REQUEST_CERTIFICATION);
    assertThat(response.getEffectType())
        .isEqualTo(com.odonta.polity.api.model.EffectType.AMEND_CONSTITUTION);
    assertThat(response.getOutcome()).isEqualTo("passed");
    assertThat(response.getActorName()).isEqualTo("Ada");
    assertThat(response.getConstitutionVersion()).isEqualTo(2);
  }

  @Test
  void mapsOfficesAndJusticeAcrossApplicationAndTransportBoundaries() {
    OfficeProjection office = mock(OfficeProjection.class);
    when(office.getCode()).thenReturn("steward");
    when(office.getName()).thenReturn("Steward");
    when(office.getTermLengthDays()).thenReturn(90);
    assertThat(officeTransport.toResponse(officeApplication.toResult(office)).getName())
        .isEqualTo("Steward");

    OfficeTermProjection term = mock(OfficeTermProjection.class);
    when(term.getStatus()).thenReturn(OfficeTermStatus.ACTIVE);
    when(term.getMemberName()).thenReturn("Ada");
    assertThat(officeTransport.toResponse(officeApplication.toResult(term)).getStatus())
        .isEqualTo(com.odonta.polity.api.model.OfficeTermStatus.ACTIVE);

    SanctionProjection sanction = mock(SanctionProjection.class);
    when(sanction.getType()).thenReturn(SanctionType.WARNING);
    when(sanction.getStatus()).thenReturn(SanctionStatus.ACTIVE);
    when(sanction.getTargetName()).thenReturn("Ada");
    assertThat(justiceTransport.toResponse(justiceApplication.toResult(sanction)).getType())
        .isEqualTo(com.odonta.polity.api.model.SanctionType.WARNING);

    AppealProjection appeal = mock(AppealProjection.class);
    when(appeal.getStatus()).thenReturn(AppealStatus.GRANTED);
    when(appeal.getAppellantName()).thenReturn("Ada");
    assertThat(justiceTransport.toResponse(justiceApplication.toResult(appeal)).getStatus())
        .isEqualTo(com.odonta.polity.api.model.AppealStatus.GRANTED);
  }

  @Test
  void mapsInvitationsAcrossTransportBoundary() {
    var request =
        new com.odonta.polity.api.model.CreateMemberInvitationRequest("friend@example.com");
    assertThat(invitationTransport.toInput(request).email()).isEqualTo("friend@example.com");

    var response =
        invitationTransport.toResponse(
            new MembershipInvitationResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Friend Republic",
                "friend@example.com",
                "Ada",
                InvitationStatus.PENDING,
                NOW,
                null));

    assertThat(response.getStatus())
        .isEqualTo(com.odonta.polity.api.model.InvitationStatus.PENDING);
    assertThat(response.getPolityName()).isEqualTo("Friend Republic");
  }
}
