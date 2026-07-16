package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.mapper.ConstitutionalReviewApplicationMapper;
import com.odonta.polity.repository.ConstitutionalReviewProjection;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.OfficialRecordProjection;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.result.ConstitutionalReviewResult;
import com.odonta.polity.result.PageResult;
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

@Service
@RequiredArgsConstructor
public class ConstitutionalReviewService {
  private final PolityAccessPolicy access;
  private final ConstitutionalReviewRepository constitutionalReviews;
  private final ConstitutionalReviewApplicationMapper mapper;
  private final MembershipService memberships;
  private final OfficialRecordRepository officialRecords;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PageResult<ConstitutionalReviewResult> list(
      UUID polityId, UUID userId, int page, int size) {
    access.requireReadable(polityId, userId);
    Page<ConstitutionalReviewProjection> pageResult =
        constitutionalReviews.findProjectionsByPolityIdOrderByDecidedAtDescIdAsc(
            polityId, PageRequest.of(page, size));
    List<ConstitutionalReviewProjection> projections = pageResult.getContent();
    if (projections.isEmpty()) {
      return new PageResult<>(
          List.of(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }
    Map<UUID, OfficialRecordProjection> recordsById =
        officialRecords
            .findProjectionsByPolityIdAndIdIn(
                polityId,
                projections.stream()
                    .map(ConstitutionalReviewProjection::getTargetRecordId)
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(OfficialRecordProjection::getId, Function.identity()));
    Map<UUID, String> memberNames =
        memberships.displayNames(
            polityId,
            projections.stream()
                .map(ConstitutionalReviewProjection::getPetitionerMembershipId)
                .toList());
    return new PageResult<>(
        projections.stream()
            .map(projection -> result(projection, recordsById, memberNames))
            .toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements());
  }

  private ConstitutionalReviewResult result(
      ConstitutionalReviewProjection projection,
      Map<UUID, OfficialRecordProjection> recordsById,
      Map<UUID, String> memberNames) {
    OfficialRecordProjection target =
        requiredTargetRecord(recordsById, projection.getTargetRecordId());
    return mapper.toResult(
        projection,
        target.getEntryNumber(),
        target.getType(),
        requiredPetitionerName(memberNames, projection.getPetitionerMembershipId()));
  }

  private OfficialRecordProjection requiredTargetRecord(
      Map<UUID, OfficialRecordProjection> recordsById, UUID targetRecordId) {
    OfficialRecordProjection target = recordsById.get(targetRecordId);
    if (target == null) {
      throw PolityResource.OFFICIAL_RECORD_ENTRY.notFound();
    }
    return target;
  }

  private String requiredPetitionerName(
      Map<UUID, String> memberNames, UUID petitionerMembershipId) {
    String petitionerName = memberNames.get(petitionerMembershipId);
    if (petitionerName == null) {
      throw PolityResource.MEMBER.notFound();
    }
    return petitionerName;
  }
}
