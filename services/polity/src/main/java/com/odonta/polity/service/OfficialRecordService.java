package com.odonta.polity.service;

import static com.odonta.polity.exception.RequiredResource.required;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.mapper.OfficialRecordApplicationMapper;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordSequence;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.repository.ConstitutionVersionProjection;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.OfficialRecordProjection;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.OfficialRecordSequenceRepository;
import com.odonta.polity.result.OfficialRecordResult;
import com.odonta.polity.result.PageResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfficialRecordService {
  private final PolityAccessPolicy access;
  private final ConstitutionVersionRepository constitutions;
  private final OfficialRecordApplicationMapper mapper;
  private final MembershipService memberships;
  private final OfficialRecordRepository records;
  private final OfficialRecordSequenceRepository sequences;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PageResult<OfficialRecordResult> list(UUID polityId, UUID userId, int page, int size) {
    access.requireReadable(polityId, userId);
    Page<OfficialRecordProjection> pageResult =
        records.findProjectionsByPolityIdOrderByEntryNumberDescIdAsc(
            polityId, PageRequest.of(page, size));
    List<OfficialRecordProjection> entries = pageResult.getContent();
    if (entries.isEmpty()) {
      return new PageResult<>(
          List.of(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }
    Map<UUID, ConstitutionVersionProjection> constitutionsById =
        constitutions
            .findProjectionsByPolityIdAndIdIn(
                polityId,
                entries.stream()
                    .map(OfficialRecordProjection::getConstitutionVersionId)
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(ConstitutionVersionProjection::getId, Function.identity()));
    Map<UUID, String> actorNames =
        memberships.displayNames(
            polityId,
            entries.stream().map(OfficialRecordProjection::getActorMembershipId).toList());
    return new PageResult<>(
        entries.stream().map(entry -> result(entry, constitutionsById, actorNames)).toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements());
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void append(
      UUID polityId,
      UUID jurisdictionId,
      UUID constitutionVersionId,
      UUID actorMembershipId,
      OfficialRecordType type,
      UUID sourceId,
      OfficialRecordContext context,
      OfficialRecordTemplate template,
      OffsetDateTime occurredAt) {
    int entryNumber = nextEntryNumber(polityId);
    records.save(
        new OfficialRecordEntry(
            polityId,
            entryNumber,
            jurisdictionId,
            constitutionVersionId,
            actorMembershipId,
            type,
            sourceId,
            context,
            template,
            occurredAt));
  }

  private OfficialRecordResult result(
      OfficialRecordProjection projection,
      Map<UUID, ConstitutionVersionProjection> constitutionsById,
      Map<UUID, String> actorNames) {
    ConstitutionVersionProjection constitution =
        required(
            constitutionsById, projection.getConstitutionVersionId(), PolityResource.CONSTITUTION);
    return mapper.toResult(
        projection,
        required(actorNames, projection.getActorMembershipId(), PolityResource.MEMBER),
        constitution.getVersion());
  }

  private int nextEntryNumber(UUID polityId) {
    OfficialRecordSequence sequence =
        sequences
            .findEntityByPolityIdForUpdate(polityId)
            .orElseGet(() -> sequences.saveAndFlush(new OfficialRecordSequence(polityId)));
    return sequence.claimNextEntryNumber();
  }
}
