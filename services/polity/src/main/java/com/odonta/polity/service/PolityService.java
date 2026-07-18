package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.resolver.PolitySummaryResolver;
import com.odonta.polity.result.PageResult;
import com.odonta.polity.result.PolitySummaryResult;
import io.github.lutzseverino.cardo.common.api.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolityService {
  private final PolityAccessPolicy access;
  private final PolityRepository polities;
  private final PolitySummaryResolver summaries;

  public PageResult<PolitySummaryResult> list(UUID userId, String query, int page, int size) {
    Page<PolityProjection> projections =
        polities.findAccessibleProjections(
            userId,
            MembershipStatus.ACTIVE,
            PolityVisibility.PUBLIC,
            normalizeQuery(query),
            PageRequest.of(page, size));
    return new PageResult<>(
        summaries.resolveAll(projections.getContent()),
        projections.getNumber(),
        projections.getSize(),
        projections.getTotalElements());
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PolitySummaryResult get(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return summaries.resolve(
        polities.findProjectedById(polityId).orElseThrow(PolityResource.POLITY::notFound));
  }

  public void requireActive(UUID polityId) {
    PolityStatus status =
        polities
            .findProjectedById(polityId)
            .map(PolityProjection::getStatus)
            .orElseThrow(PolityResource.POLITY::notFound);
    if (status != PolityStatus.ACTIVE) {
      throw ApiException.conflict(
          "polity_disbanded", "This polity has been disbanded and no longer accepts actions.");
    }
  }

  public void requireDisbandmentGovernment(UUID polityId) {
    requireActive(polityId);
  }

  private String normalizeQuery(String query) {
    return query == null || query.isBlank() ? null : query.trim();
  }
}
