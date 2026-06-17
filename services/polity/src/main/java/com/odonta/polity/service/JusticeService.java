package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.JusticeApplicationMapper;
import com.odonta.polity.model.AppealResult;
import com.odonta.polity.model.SanctionResult;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.SanctionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JusticeService {
  private final PolityAccessPolicy access;
  private final Clock clock;
  private final AppealRepository appeals;
  private final JusticeApplicationMapper mapper;
  private final SanctionRepository sanctions;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<SanctionResult> sanctions(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    OffsetDateTime now = OffsetDateTime.now(clock);
    return mapper.toSanctionResults(sanctions.findProjectionsByPolityId(polityId, now));
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<AppealResult> appeals(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return mapper.toAppealResults(appeals.findProjectionsByPolityId(polityId));
  }
}
