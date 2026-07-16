package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.mapper.AppealApplicationMapper;
import com.odonta.polity.repository.AppealProjection;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.result.AppealResult;
import com.odonta.polity.result.PageResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppealService {
  private final PolityAccessPolicy access;
  private final AppealRepository appeals;
  private final AppealApplicationMapper mapper;
  private final MembershipService memberships;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PageResult<AppealResult> list(UUID polityId, UUID userId, int page, int size) {
    access.requireReadable(polityId, userId);
    Page<AppealProjection> pageResult =
        appeals.findProjectionsByPolityIdOrderByDecidedAtDescIdAsc(
            polityId, PageRequest.of(page, size));
    List<AppealProjection> projections = pageResult.getContent();
    if (projections.isEmpty()) {
      return new PageResult<>(
          List.of(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }
    Map<UUID, String> memberNames =
        memberships.displayNames(
            polityId,
            projections.stream().map(AppealProjection::getAppellantMembershipId).toList());
    return new PageResult<>(
        projections.stream()
            .map(
                projection ->
                    mapper.toResult(
                        projection,
                        requiredAppellantName(memberNames, projection.getAppellantMembershipId())))
            .toList(),
        pageResult.getNumber(),
        pageResult.getSize(),
        pageResult.getTotalElements());
  }

  private String requiredAppellantName(Map<UUID, String> memberNames, UUID appellantMembershipId) {
    String appellantName = memberNames.get(appellantMembershipId);
    if (appellantName == null) {
      throw PolityResource.MEMBER.notFound();
    }
    return appellantName;
  }
}
