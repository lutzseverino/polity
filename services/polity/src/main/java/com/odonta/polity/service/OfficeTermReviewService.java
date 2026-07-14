package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficeTermReviewApplicationMapper;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProjection;
import com.odonta.polity.repository.OfficeTermReviewRepository;
import com.odonta.polity.result.OfficeTermReviewResult;
import com.odonta.polity.result.PageResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfficeTermReviewService {
  private final PolityAccessPolicy access;
  private final OfficeTermReviewApplicationMapper mapper;
  private final MembershipService memberships;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficeTermReviewRepository reviews;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PageResult<OfficeTermReviewResult> list(UUID polityId, UUID userId, int page, int size) {
    access.requireReadable(polityId, userId);
    Page<OfficeTermReviewProjection> pageResult =
        reviews.findProjectionsByPolityIdOrderByDecidedAtDescIdAsc(
            polityId, PageRequest.of(page, size));
    List<OfficeTermReviewProjection> projections = pageResult.getContent();
    if (projections.isEmpty()) {
      return new PageResult<>(
          List.of(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }
    Map<UUID, OfficeTermProjection> termsById =
        officeTerms
            .findProjectionsByPolityIdAndIdIn(
                polityId,
                projections.stream()
                    .map(OfficeTermReviewProjection::getOfficeTermId)
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(OfficeTermProjection::getId, Function.identity()));
    Map<UUID, OfficeProjection> officesById =
        offices
            .findProjectionsByPolityIdAndIdIn(
                polityId,
                termsById.values().stream()
                    .map(OfficeTermProjection::getOfficeId)
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(OfficeProjection::getId, Function.identity()));
    Map<UUID, String> memberNames =
        memberships.displayNames(
            polityId,
            Stream.concat(
                    projections.stream().map(OfficeTermReviewProjection::getPetitionerMembershipId),
                    termsById.values().stream().map(OfficeTermProjection::getMembershipId))
                .toList());
    return new PageResult<>(
        projections.stream()
            .map(projection -> result(projection, termsById, officesById, memberNames))
            .toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements());
  }

  private OfficeTermReviewResult result(
      OfficeTermReviewProjection projection,
      Map<UUID, OfficeTermProjection> termsById,
      Map<UUID, OfficeProjection> officesById,
      Map<UUID, String> memberNames) {
    OfficeTermProjection term = requiredOfficeTerm(termsById, projection.getOfficeTermId());
    OfficeProjection office = requiredOffice(officesById, term.getOfficeId());
    return mapper.toResult(
        projection,
        requiredMemberName(memberNames, projection.getPetitionerMembershipId()),
        term.getMembershipId(),
        requiredMemberName(memberNames, term.getMembershipId()),
        office.getName(),
        office.getNameKey());
  }

  private OfficeTermProjection requiredOfficeTerm(
      Map<UUID, OfficeTermProjection> termsById, UUID officeTermId) {
    OfficeTermProjection term = termsById.get(officeTermId);
    if (term == null) {
      throw ApiException.notFound("office_term_not_found", "Office term not found.");
    }
    return term;
  }

  private OfficeProjection requiredOffice(Map<UUID, OfficeProjection> officesById, UUID officeId) {
    OfficeProjection office = officesById.get(officeId);
    if (office == null) {
      throw ApiException.notFound("office_not_found", "Office not found.");
    }
    return office;
  }

  private String requiredMemberName(Map<UUID, String> memberNames, UUID membershipId) {
    String memberName = memberNames.get(membershipId);
    if (memberName == null) {
      throw ApiException.notFound("member_not_found", "Member not found.");
    }
    return memberName;
  }
}
