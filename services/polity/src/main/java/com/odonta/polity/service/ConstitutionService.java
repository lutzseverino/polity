package com.odonta.polity.service;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.mapper.ConstitutionApplicationMapper;
import com.odonta.polity.mapper.ConstitutionalPowerApplicationMapper;
import com.odonta.polity.mapper.InstitutionApplicationMapper;
import com.odonta.polity.mapper.OfficeApplicationMapper;
import com.odonta.polity.mapper.ProcedureApplicationMapper;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.result.ConstitutionResult;
import com.odonta.polity.result.ProcedureResult;
import java.util.Comparator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConstitutionService {
  private final PolityAccessPolicy access;
  private final ConstitutionApplicationMapper mapper;
  private final ConstitutionVersionRepository constitutions;
  private final InstitutionApplicationMapper institutionMapper;
  private final InstitutionRepository institutions;
  private final ProcedureApplicationMapper procedureMapper;
  private final ProcedureRepository procedures;
  private final OfficeApplicationMapper officeMapper;
  private final OfficeRepository offices;
  private final ConstitutionalPowerApplicationMapper powerMapper;
  private final ConstitutionalPowerRepository powers;

  @Transactional(readOnly = true)
  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public ConstitutionResult get(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    ConstitutionVersion constitution =
        constitutions
            .findEntityByPolityIdAndStatus(polityId, ConstitutionStatus.RATIFIED)
            .orElseThrow(PolityResource.CONSTITUTION::notFound);
    return mapper.toResult(
        constitution,
        institutions.findProjectionsByConstitutionVersionId(constitution.getId()).stream()
            .map(institutionMapper::toResult)
            .sorted(Comparator.comparing(result -> result.kind().name()))
            .toList(),
        procedures.findProjectionsByConstitutionVersionId(constitution.getId()).stream()
            .map(procedureMapper::toResult)
            .sorted(Comparator.comparing(ProcedureResult::code))
            .toList(),
        officeMapper.toResults(
            offices.findProjectionsByConstitutionVersionIdOrderByNameAscIdAsc(
                constitution.getId())),
        powers.findProjectionsByConstitutionVersionId(constitution.getId()).stream()
            .map(powerMapper::toResult)
            .sorted(Comparator.comparing(result -> result.code().name()))
            .toList());
  }
}
