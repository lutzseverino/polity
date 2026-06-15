package com.odonta.polity.service;

import com.odonta.polity.mapper.OfficialRecordApplicationMapper;
import com.odonta.polity.model.OfficialRecordResult;
import com.odonta.polity.repository.OfficialRecordRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfficialRecordService {
  private final MembershipReader membershipReader;
  private final OfficialRecordApplicationMapper mapper;
  private final OfficialRecordRepository records;

  public List<OfficialRecordResult> list(UUID polityId, UUID userId) {
    membershipReader.active(polityId, userId);
    return mapper.toResults(records.findProjectionsByPolityId(polityId));
  }
}
