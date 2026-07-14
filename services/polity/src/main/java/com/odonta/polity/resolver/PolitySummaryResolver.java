package com.odonta.polity.resolver;

import com.odonta.common.api.ApiException;
import com.odonta.polity.mapper.PolityApplicationMapper;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.repository.ConstitutionVersionProjection;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.InstitutionProjection;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionProjection;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.result.PolitySummaryResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolitySummaryResolver {
  private final PolityApplicationMapper mapper;
  private final ConstitutionVersionRepository constitutions;
  private final JurisdictionRepository jurisdictions;
  private final InstitutionRepository institutions;

  public List<PolitySummaryResult> resolveAll(List<PolityProjection> polities) {
    if (polities.isEmpty()) {
      return List.of();
    }
    List<UUID> polityIds = polities.stream().map(PolityProjection::getId).distinct().toList();
    Map<UUID, ConstitutionVersionProjection> constitutionByPolity =
        constitutions
            .findProjectionsByPolityIdInAndStatus(polityIds, ConstitutionStatus.RATIFIED)
            .stream()
            .collect(
                Collectors.toMap(
                    ConstitutionVersionProjection::getPolityId,
                    Function.identity(),
                    (first, ignored) -> first));
    Map<UUID, JurisdictionProjection> jurisdictionByPolity =
        jurisdictions.findProjectionsByPolityIdInAndKind(polityIds, JurisdictionKind.ROOT).stream()
            .collect(
                Collectors.toMap(
                    JurisdictionProjection::getPolityId,
                    Function.identity(),
                    (first, ignored) -> first));
    List<UUID> constitutionIds =
        polityIds.stream()
            .map(constitutionByPolity::get)
            .filter(Objects::nonNull)
            .map(ConstitutionVersionProjection::getId)
            .distinct()
            .toList();
    Map<UUID, InstitutionProjection> institutionByConstitution =
        constitutionIds.isEmpty()
            ? Map.of()
            : institutions.findProjectionsByConstitutionVersionIdIn(constitutionIds).stream()
                .collect(
                    Collectors.toMap(
                        InstitutionProjection::getConstitutionVersionId,
                        Function.identity(),
                        PolitySummaryResolver::preferredInstitution));
    return polities.stream()
        .map(
            polity -> {
              ConstitutionVersionProjection constitution =
                  required(
                      constitutionByPolity,
                      polity.getId(),
                      "constitution_not_found",
                      "Constitution not found.");
              JurisdictionProjection jurisdiction =
                  required(
                      jurisdictionByPolity,
                      polity.getId(),
                      "jurisdiction_not_found",
                      "Jurisdiction not found.");
              InstitutionProjection institution =
                  required(
                      institutionByConstitution,
                      constitution.getId(),
                      "institution_not_found",
                      "Institution not found.");
              return mapper.toSummary(
                  polity,
                  constitution.getVersion(),
                  jurisdiction.getName(),
                  institution.getName(),
                  institution.getNameKey());
            })
        .toList();
  }

  public PolitySummaryResult resolve(PolityProjection polity) {
    return resolveAll(List.of(polity)).getFirst();
  }

  private static InstitutionProjection preferredInstitution(
      InstitutionProjection first, InstitutionProjection second) {
    return institutionComparator().compare(first, second) <= 0 ? first : second;
  }

  private static Comparator<InstitutionProjection> institutionComparator() {
    return Comparator.comparingInt(
            (InstitutionProjection institution) -> institutionPriority(institution.getKind()))
        .thenComparing(InstitutionProjection::getId);
  }

  private static int institutionPriority(InstitutionKind kind) {
    return switch (kind) {
      case COUNCIL -> 0;
      case ASSEMBLY -> 1;
      default -> 2;
    };
  }

  private <K, V> V required(Map<K, V> values, K key, String code, String message) {
    V value = values.get(key);
    if (value == null) {
      throw ApiException.notFound(code, message);
    }
    return value;
  }
}
