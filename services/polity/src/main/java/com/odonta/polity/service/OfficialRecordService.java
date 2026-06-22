package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficialRecordApplicationMapper;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordResult;
import com.odonta.polity.model.OfficialRecordSequence;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.OfficialRecordProjection;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.OfficialRecordSequenceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
  public List<OfficialRecordResult> list(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return records.findProjectionsByPolityIdOrderByEntryNumberDesc(polityId).stream()
        .map(this::result)
        .toList();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  void append(
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

  private OfficialRecordResult result(OfficialRecordProjection projection) {
    ConstitutionVersion constitution =
        constitutions
            .findById(projection.getConstitutionVersionId())
            .orElseThrow(
                () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
    return mapper.toResult(
        projection,
        memberships.displayName(projection.getActorMembershipId()),
        constitution.getVersion());
  }

  private int nextEntryNumber(UUID polityId) {
    OfficialRecordSequence sequence =
        sequences
            .findByPolityIdForUpdate(polityId)
            .orElseGet(() -> sequences.saveAndFlush(new OfficialRecordSequence(polityId)));
    return sequence.claimNextEntryNumber();
  }
}
