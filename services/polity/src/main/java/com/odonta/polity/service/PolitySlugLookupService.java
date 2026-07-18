package com.odonta.polity.service;

import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.result.PolitySummaryResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolitySlugLookupService {
  private final PolityRepository polityRepository;
  private final PolityService polities;

  public PolitySummaryResult get(String slug, UUID userId) {
    PolityProjection polity =
        polityRepository.findProjectedBySlug(slug).orElseThrow(PolityResource.POLITY::notFound);
    return polities.get(polity.getId(), userId);
  }
}
