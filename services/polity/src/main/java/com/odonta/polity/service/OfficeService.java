package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficeApplicationMapper;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.resolver.PolityContextResolver;
import com.odonta.polity.result.OfficeResult;
import com.odonta.polity.result.PageResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfficeService {
  private final PolityAccessPolicy access;
  private final OfficeApplicationMapper officeMapper;
  private final OfficeRepository offices;
  private final PolityContextResolver polityContext;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PageResult<OfficeResult> list(UUID polityId, UUID userId, int page, int size) {
    access.requireReadable(polityId, userId);
    ConstitutionVersion constitution = polityContext.constitution(polityId);
    Page<OfficeProjection> projections =
        offices.findProjectionsByConstitutionVersionIdOrderByNameAscIdAsc(
            constitution.getId(), PageRequest.of(page, size));
    return new PageResult<>(
        officeMapper.toResults(projections.getContent()),
        projections.getNumber(),
        projections.getSize(),
        projections.getTotalElements());
  }
}
