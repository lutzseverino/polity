package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficeTermApplicationMapper;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.result.OfficeTermResult;
import com.odonta.polity.result.PageResult;
import java.time.Clock;
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

@Service
@RequiredArgsConstructor
public class OfficeTermService {
  private final Clock clock;
  private final PolityAccessPolicy access;
  private final OfficeTermApplicationMapper mapper;
  private final MembershipService memberships;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PageResult<OfficeTermResult> list(UUID polityId, UUID userId, int page, int size) {
    access.requireReadable(polityId, userId);
    OffsetDateTime now = OffsetDateTime.now(clock);
    Page<OfficeTermProjection> pageResult =
        officeTerms.findProjectionsByPolityIdOrderByStartedAtDescIdAsc(
            polityId, PageRequest.of(page, size));
    List<OfficeTermProjection> projections = pageResult.getContent();
    if (projections.isEmpty()) {
      return new PageResult<>(
          List.of(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }
    Map<UUID, OfficeProjection> officesById =
        offices
            .findProjectionsByPolityIdAndIdIn(
                polityId,
                projections.stream().map(OfficeTermProjection::getOfficeId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(OfficeProjection::getId, Function.identity()));
    Map<UUID, String> memberNames =
        memberships.displayNames(
            polityId,
            projections.stream().map(OfficeTermProjection::getMembershipId).distinct().toList());
    return new PageResult<>(
        projections.stream()
            .map(projection -> result(projection, officesById, memberNames, now))
            .toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements());
  }

  private OfficeTermResult result(
      OfficeTermProjection projection,
      Map<UUID, OfficeProjection> officesById,
      Map<UUID, String> memberNames,
      OffsetDateTime now) {
    OfficeTermStatus status =
        projection.getStatus() == OfficeTermStatus.ACTIVE && !projection.getEndsAt().isAfter(now)
            ? OfficeTermStatus.ENDED
            : projection.getStatus();
    OfficeProjection office = requiredOffice(projection.getOfficeId(), officesById);
    return mapper.toResult(
        projection,
        office.getName(),
        office.getNameKey(),
        requiredMemberName(projection.getMembershipId(), memberNames),
        status);
  }

  private OfficeProjection requiredOffice(UUID officeId, Map<UUID, OfficeProjection> officesById) {
    OfficeProjection office = officesById.get(officeId);
    if (office == null) {
      throw ApiException.notFound("office_not_found", "Office not found.");
    }
    return office;
  }

  private String requiredMemberName(UUID membershipId, Map<UUID, String> memberNames) {
    String memberName = memberNames.get(membershipId);
    if (memberName == null) {
      throw ApiException.notFound("member_not_found", "Member not found.");
    }
    return memberName;
  }
}
