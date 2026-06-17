package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.OfficialRecordApplicationMapper;
import com.odonta.polity.model.OfficialRecordResult;
import com.odonta.polity.repository.OfficialRecordRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfficialRecordService {
  private final PolityAccessPolicy access;
  private final OfficialRecordApplicationMapper mapper;
  private final OfficialRecordRepository records;

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<OfficialRecordResult> list(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return mapper.toResults(records.findProjectionsByPolityId(polityId));
  }
}
