package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.model.OfficialRecordDetails;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficialRecordRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfficialRecordService {
  private final ConstitutionVersionRepository constitutions;
  private final MembershipReader membershipReader;
  private final MembershipRepository memberships;
  private final OfficialRecordRepository records;

  public List<OfficialRecordDetails> list(UUID polityId, UUID userId) {
    membershipReader.active(polityId, userId);
    return records.findByPolityIdOrderByOccurredAtDesc(polityId).stream()
        .map(
            entry ->
                new OfficialRecordDetails(
                    entry,
                    memberships
                        .findById(entry.getActorMembershipId())
                        .orElseThrow(
                            () -> ApiException.notFound("member_not_found", "Member not found.")),
                    constitutions
                        .findById(entry.getConstitutionVersionId())
                        .orElseThrow(
                            () ->
                                ApiException.notFound(
                                    "constitution_not_found", "Constitution not found."))))
        .toList();
  }
}
