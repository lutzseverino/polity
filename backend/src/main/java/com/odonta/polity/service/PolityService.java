package com.odonta.polity.service;

import com.odonta.authorization.grant.Grants;
import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.identity.client.IdentityUser;
import com.odonta.identity.client.IdentityUsersClient;
import com.odonta.identity.client.ProvisionalUser;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.model.AdmitMemberCommand;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.CreatePolityCommand;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PolityDetails;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class PolityService {
  private static final String STARTER_CONSTITUTION =
      "All active citizens may admit citizens, introduce ordinary resolutions, vote, and request certification. "
          + "Ordinary resolutions require participation by at least half of the eligible electorate and pass when yes votes exceed no votes. Abstentions count toward quorum.";

  private final Clock clock;
  private final ConstitutionalAuthority authority;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalPowerRepository powers;
  private final Grants grants;
  private final IdentityUsersClient identityUsers;
  private final InstitutionRepository institutions;
  private final JurisdictionRepository jurisdictions;
  private final MembershipReader membershipReader;
  private final MembershipRepository memberships;
  private final OfficialRecordWriter record;
  private final PolityGrantPlanner grantPlanner;
  private final PolityRepository polities;
  private final ProcedureRepository procedures;

  @Transactional
  public PolityDetails create(AuthenticatedUser founder, @Valid CreatePolityCommand command) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    IdentityUser identity = identityUsers.get(founder.id());
    Polity polity = polities.saveAndFlush(new Polity(command.name(), founder.id()));
    Jurisdiction jurisdiction =
        jurisdictions.saveAndFlush(
            new Jurisdiction(polity.getId(), command.name(), JurisdictionKind.ROOT));
    ConstitutionVersion constitution =
        constitutions.saveAndFlush(
            new ConstitutionVersion(
                polity.getId(), 1, "Starter Constitution", STARTER_CONSTITUTION, now));
    Institution institution =
        institutions.saveAndFlush(
            new Institution(
                polity.getId(),
                jurisdiction.getId(),
                constitution.getId(),
                "Citizens' Assembly",
                InstitutionKind.ASSEMBLY));
    procedures.saveAndFlush(
        new Procedure(
            polity.getId(),
            constitution.getId(),
            institution.getId(),
            Procedure.ORDINARY_RESOLUTION,
            "Ordinary resolution",
            1,
            2,
            VotingThreshold.SIMPLE_MAJORITY,
            EffectType.ADOPT_RESOLUTION));
    seedPowers(polity.getId(), constitution.getId());
    Membership membership =
        memberships.saveAndFlush(
            new Membership(
                polity.getId(),
                identity.id(),
                identity.authorizationSubject(),
                identity.email(),
                displayName(identity),
                now,
                null));
    grants.stage(grantPlanner.founder(founder.authorizationSubject(), polity.getId()));
    record.append(
        polity.getId(),
        jurisdiction.getId(),
        constitution.getId(),
        membership.getId(),
        OfficialRecordType.POLITY_FOUNDED,
        polity.getId(),
        command.name() + " was founded",
        "The polity was founded with a root jurisdiction and citizens' assembly.",
        now);
    record.append(
        polity.getId(),
        jurisdiction.getId(),
        constitution.getId(),
        membership.getId(),
        OfficialRecordType.CONSTITUTION_RATIFIED,
        constitution.getId(),
        "Starter Constitution ratified",
        STARTER_CONSTITUTION,
        now);
    return new PolityDetails(polity, constitution, jurisdiction, institution);
  }

  public List<PolityDetails> list(UUID userId) {
    List<UUID> polityIds =
        memberships.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE).stream()
            .map(Membership::getPolityId)
            .toList();
    return polities.findByIdInOrderByCreatedAtDesc(polityIds).stream().map(this::details).toList();
  }

  public PolityDetails get(UUID polityId, UUID userId) {
    membershipReader.active(polityId, userId);
    return details(getPolity(polityId));
  }

  public List<Membership> members(UUID polityId, UUID userId) {
    membershipReader.active(polityId, userId);
    return memberships.findByPolityIdAndStatusOrderByAdmittedAtAsc(
        polityId, MembershipStatus.ACTIVE);
  }

  @Transactional
  public Membership admit(
      UUID polityId, AuthenticatedUser actor, @Valid AdmitMemberCommand command) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Membership admittingMember = membershipReader.active(polityId, actor.id());
    ConstitutionVersion constitution = constitution(polityId);
    authority.require(admittingMember, constitution, PowerCode.ADMIT_MEMBER);
    ProvisionalUser identity = identityUsers.createProvisional(command.email());
    if (memberships.existsByPolityIdAndUserId(polityId, identity.id())) {
      throw ApiException.conflict("member_exists", "This user is already a member.");
    }
    Membership admitted =
        memberships.saveAndFlush(
            new Membership(
                polityId,
                identity.id(),
                identity.authorizationSubject(),
                command.email(),
                command.email(),
                now,
                admittingMember.getId()));
    grants.stage(grantPlanner.membership(identity.authorizationSubject(), polityId));
    Jurisdiction jurisdiction = jurisdiction(polityId);
    record.append(
        polityId,
        jurisdiction.getId(),
        constitution.getId(),
        admittingMember.getId(),
        OfficialRecordType.MEMBER_ADMITTED,
        admitted.getId(),
        admitted.getDisplayName() + " was admitted",
        "%s admitted %s as an active citizen under the Starter Constitution."
            .formatted(admittingMember.getDisplayName(), admitted.getDisplayName()),
        now);
    return admitted;
  }

  private void seedPowers(UUID polityId, UUID constitutionId) {
    powers.saveAllAndFlush(
        List.of(
            new ConstitutionalPower(
                polityId,
                constitutionId,
                PowerCode.ADMIT_MEMBER,
                "Admit citizens",
                PowerHolderScope.ACTIVE_MEMBER),
            new ConstitutionalPower(
                polityId,
                constitutionId,
                PowerCode.INTRODUCE_MOTION,
                "Introduce resolutions",
                PowerHolderScope.ACTIVE_MEMBER),
            new ConstitutionalPower(
                polityId,
                constitutionId,
                PowerCode.REQUEST_CERTIFICATION,
                "Request certification",
                PowerHolderScope.ACTIVE_MEMBER)));
  }

  private String displayName(IdentityUser user) {
    return user.name() == null || user.name().isBlank() ? user.email() : user.name();
  }

  private Polity getPolity(UUID polityId) {
    return polities
        .findById(polityId)
        .orElseThrow(() -> ApiException.notFound("polity_not_found", "Polity not found."));
  }

  private PolityDetails details(Polity polity) {
    return new PolityDetails(
        polity,
        constitution(polity.getId()),
        jurisdiction(polity.getId()),
        institution(polity.getId()));
  }

  ConstitutionVersion constitution(UUID polityId) {
    return constitutions
        .findByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED)
        .orElseThrow(
            () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
  }

  Jurisdiction jurisdiction(UUID polityId) {
    return jurisdictions
        .findByPolityIdAndKind(polityId, JurisdictionKind.ROOT)
        .orElseThrow(
            () -> ApiException.notFound("jurisdiction_not_found", "Jurisdiction not found."));
  }

  Institution institution(UUID polityId) {
    return institutions
        .findByPolityIdAndKind(polityId, InstitutionKind.ASSEMBLY)
        .orElseThrow(
            () -> ApiException.notFound("institution_not_found", "Institution not found."));
  }
}
