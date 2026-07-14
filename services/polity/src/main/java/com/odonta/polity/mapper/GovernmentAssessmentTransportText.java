package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ConstitutionalHealthDiagnosticResponse;
import com.odonta.polity.api.model.GovernmentReadinessDiagnosticResponse;
import com.odonta.polity.model.ConstitutionalHealthDiagnostic;
import com.odonta.polity.model.GovernmentReadinessDiagnostic;
import com.odonta.polity.result.ConstitutionalHealthResult;
import com.odonta.polity.result.GovernmentReadinessResult;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GovernmentAssessmentTransportText {
  private final TransportTextResolver text;

  @Named("readinessStatusMessage")
  public String readinessStatusMessage(GovernmentReadinessResult result) {
    return result == null || result.status() == null
        ? null
        : text.resolveName(
            key("government_readiness.status", result.status()), result.status().name());
  }

  @Named("readinessDiagnostics")
  public List<GovernmentReadinessDiagnosticResponse> readinessDiagnostics(
      GovernmentReadinessResult result) {
    return result == null || result.diagnostics() == null
        ? List.of()
        : result.diagnostics().stream().map(this::readinessDiagnostic).toList();
  }

  @Named("constitutionalHealthStatusMessage")
  public String constitutionalHealthStatusMessage(ConstitutionalHealthResult result) {
    return result == null || result.status() == null
        ? null
        : text.resolveName(
            key("constitutional_health.status", result.status()), result.status().name());
  }

  @Named("constitutionalHealthDiagnostics")
  public List<ConstitutionalHealthDiagnosticResponse> constitutionalHealthDiagnostics(
      ConstitutionalHealthResult result) {
    return result == null || result.diagnostics() == null
        ? List.of()
        : result.diagnostics().stream().map(this::constitutionalHealthDiagnostic).toList();
  }

  private GovernmentReadinessDiagnosticResponse readinessDiagnostic(
      GovernmentReadinessDiagnostic diagnostic) {
    String value = diagnostic.name().toLowerCase(Locale.ROOT);
    return new GovernmentReadinessDiagnosticResponse(
        com.odonta.polity.api.model.GovernmentReadinessDiagnostic.fromValue(value),
        text.resolveName("government_readiness.diagnostic." + value, diagnostic.name()));
  }

  private ConstitutionalHealthDiagnosticResponse constitutionalHealthDiagnostic(
      ConstitutionalHealthDiagnostic diagnostic) {
    String value = diagnostic.name().toLowerCase(Locale.ROOT);
    return new ConstitutionalHealthDiagnosticResponse(
        com.odonta.polity.api.model.ConstitutionalHealthDiagnostic.fromValue(value),
        text.resolveName("constitutional_health.diagnostic." + value, diagnostic.name()));
  }

  private String key(String prefix, Enum<?> value) {
    return prefix + "." + value.name().toLowerCase(Locale.ROOT);
  }
}
