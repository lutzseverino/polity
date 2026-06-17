package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficeApplicationMapper;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.OfficeResult;
import com.odonta.polity.model.OfficeTermResult;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfficeService {
  private final PolityAccessPolicy access;
  private final OfficeApplicationMapper mapper;
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
    return mapper.toTermResults(terms.findProjectionsByPolityId(polityId));
  }
}
