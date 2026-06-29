package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficeApplicationMapper;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.OfficeResult;
import com.odonta.polity.model.OfficeTermResult;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficeTermRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfficeService {
  private final Clock clock;
  private final PolityAccessPolicy access;
  private final OfficeApplicationMapper mapper;
  private final MembershipService memberships;
  private final OfficeRepository offices;
  private final OfficeTermRepository terms;
  private final PolityService polities;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<OfficeResult> list(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    ConstitutionVersion constitution = polities.constitution(polityId);
    return mapper.toResults(
        offices.findProjectionsByConstitutionVersionIdOrderByName(constitution.getId()));
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<OfficeTermResult> terms(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    OffsetDateTime now = OffsetDateTime.now(clock);
    ConstitutionVersion constitution = polities.constitution(polityId);
    Map<String, OfficeProjection> currentOffices =
        offices.findProjectionsByConstitutionVersionIdOrderByName(constitution.getId()).stream()
            .collect(Collectors.toMap(OfficeProjection::getCode, Function.identity()));
    return terms.findProjectionsByPolityIdOrderByStartedAtDesc(polityId).stream()
        .map(term -> term(term, currentOffices, now))
        .toList();
  }

  private OfficeTermResult term(
      OfficeTermProjection projection,
      Map<String, OfficeProjection> currentOffices,
      OffsetDateTime now) {
    OfficeTermStatus status =
        projection.getStatus() == OfficeTermStatus.ACTIVE && !projection.getEndsAt().isAfter(now)
            ? OfficeTermStatus.ENDED
            : projection.getStatus();
    OfficeProjection office = currentOffice(projection, currentOffices);
    return mapper.toResult(
        projection,
        office.getId(),
        office.getName(),
        office.getNameKey(),
        memberships.displayName(projection.getMembershipId()),
        status);
  }

  private OfficeProjection currentOffice(
      OfficeTermProjection projection, Map<String, OfficeProjection> currentOffices) {
    OfficeProjection office = currentOffices.get(projection.getOfficeCode());
    if (office != null) {
      return office;
    }
    return offices
        .findProjectedById(projection.getOfficeId())
        .orElseThrow(() -> ApiException.notFound("office_not_found", "Office not found."));
  }
}
